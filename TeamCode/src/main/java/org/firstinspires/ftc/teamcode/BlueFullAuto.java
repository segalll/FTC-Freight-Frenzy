package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.constraints.MecanumVelocityConstraint;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.drive.DriveConstants;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;

import org.firstinspires.ftc.teamcode.BarcodeDeterminer.BarcodeDeterminationPipeline;
import org.firstinspires.ftc.teamcode.BarcodeDeterminer.BarcodeDeterminationPipeline.BarcodePosition;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

/*
 * This is our blue-side autonomous routine.
 */
@Config
@Autonomous(group = "drive")
public class BlueFullAuto extends LinearOpMode {
    private DcMotor carouselDrive;
    private DcMotor intakeDrive;
    private Servo pivotServo;
    private SlidePIDController slideController;

    private OpenCvInternalCamera phoneCam;
    private BarcodeDeterminationPipeline pipeline;

    private DistanceSensor chuteProximitySensor;

    enum State {
        TRAJECTORY_1,
        TRAJECTORY_2,
        IDLE
    }

    @Override
    public void runOpMode() throws InterruptedException {
        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);

        carouselDrive = hardwareMap.get(DcMotor.class, "carousel");
        intakeDrive = hardwareMap.get(DcMotor.class, "intake");
        pivotServo = hardwareMap.get(Servo.class, "pivot");
        intakeDrive.setDirection(DcMotor.Direction.REVERSE);

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        pipeline = new BarcodeDeterminationPipeline();
        phoneCam.setPipeline(pipeline);

        phoneCam.setViewportRenderingPolicy(OpenCvCamera.ViewportRenderingPolicy.OPTIMIZE_VIEW);

        phoneCam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                phoneCam.startStreaming(320,240, OpenCvCameraRotation.SIDEWAYS_LEFT);
            }

            @Override
            public void onError(int errorCode) {
                /*
                 * This will be called if the camera could not be opened
                 */
            }
        });

        slideController = new SlidePIDController(hardwareMap);

        chuteProximitySensor = hardwareMap.get(DistanceSensor.class, "chuteSensor");

        waitForStart();

        BarcodePosition result = pipeline.getAnalysis();

        telemetry.addData("Analysis", result);
        telemetry.update();

        phoneCam.stopStreaming();
        phoneCam.closeCameraDevice();

        Pose2d p1 = new Pose2d(-35.5, 62.125, Math.toRadians(-90));
        TrajectorySequence t1;
        Pose2d p2;
        if (result == BarcodePosition.LEFT) {
            t1 = drive.trajectorySequenceBuilder(p1)
                    .addTemporalMarker(() ->
                            slideController.setTarget(5)
                    )
                    .splineToLinearHeading(new Pose2d(-60, 27.5, Math.toRadians(90)), Math.toRadians(-90))
                    .lineTo(new Vector2d(-31, 24.5))
                    .build();
            p2 = new Pose2d(-31, 24.5, Math.toRadians(90));
        } else if (result == BarcodePosition.CENTER) {
            t1 = drive.trajectorySequenceBuilder(p1)
                    .addTemporalMarker(() ->
                            slideController.setTarget(10)
                    )
                    .splineToLinearHeading(new Pose2d(-60, 27.5, Math.toRadians(90)), Math.toRadians(-90))
                    .lineTo(new Vector2d(-34.5, 24.5))
                    .build();
            p2 = new Pose2d(-34.5, 24.5, Math.toRadians(90));
        } else {
            t1 = drive.trajectorySequenceBuilder(p1)
                    .addTemporalMarker(() ->
                            slideController.setTarget(15)
                    )
                    .splineToLinearHeading(new Pose2d(-60, 27.5, Math.toRadians(90)), Math.toRadians(-90))
                    .lineTo(new Vector2d(-34.5, 24.5))
                    .build();
            p2 = new Pose2d(-34.5, 24.5, Math.toRadians(90));
        }

        TrajectorySequence t2 = drive.trajectorySequenceBuilder(p2)
                .addTemporalMarker(() ->
                        pivotServo.setPosition(0.97)
                )
                .UNSTABLE_addTemporalMarkerOffset(2, () ->
                        pivotServo.setPosition(0.52)
                )
                .UNSTABLE_addTemporalMarkerOffset(2.5, () ->
                        slideController.setTarget(0.03)
                )
                .waitSeconds(2)
                .lineTo(new Vector2d(-66, 24.5)) // contact at -64.25
                .setVelConstraint(new MecanumVelocityConstraint(20, DriveConstants.TRACK_WIDTH))
                .lineTo(new Vector2d(-66, 56)) // contact at 53.675?
                .resetVelConstraint()
                .addTemporalMarker(() ->
                        carouselDrive.setPower(0.6)
                )
                .UNSTABLE_addTemporalMarkerOffset(3, () ->
                        carouselDrive.setPower(0)
                )
                .waitSeconds(3.2)
                .lineToLinearHeading(new Pose2d(-30, 56, Math.toRadians(0)))
                .lineTo(new Vector2d(-30, 68)) // contact at 64.25
                .lineTo(new Vector2d(40, 68))
                .build();

        pivotServo.setPosition(0.51);

        State currentState = State.TRAJECTORY_1;
        drive.setPoseEstimate(p1);
        drive.followTrajectorySequenceAsync(t1);

        while (opModeIsActive() && !isStopRequested()) {
            switch (currentState) {
                case TRAJECTORY_1:
                    if (!drive.isBusy()) {
                        currentState = State.TRAJECTORY_2;
                        drive.setPoseEstimate(p2);
                        drive.followTrajectorySequenceAsync(t2);
                    }
                    break;
                case TRAJECTORY_2:
                    if (!drive.isBusy()) {
                        currentState = State.IDLE;
                    }
                    break;
                case IDLE:
                    break;
            }

            drive.update();
            slideController.update();
        }
    }
}