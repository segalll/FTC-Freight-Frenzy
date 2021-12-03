package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="Mecanum Linear OpMode", group="Linear Opmode")
public class MecanumDrive extends LinearOpMode {
    private final ElapsedTime runtime = new ElapsedTime();
    private DcMotor leftFrontDrive;
    private DcMotor rightFrontDrive;
    private DcMotor leftRearDrive;
    private DcMotor rightRearDrive;
    private DcMotor carouselDrive;
    private DcMotor slideDrive;
    private DcMotor intakeDrive;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");
        telemetry.update();

        leftFrontDrive = hardwareMap.get(DcMotor.class, "leftFront");
        rightFrontDrive = hardwareMap.get(DcMotor.class, "rightFront");
        leftRearDrive = hardwareMap.get(DcMotor.class, "leftRear");
        rightRearDrive = hardwareMap.get(DcMotor.class, "rightRear");
        carouselDrive = hardwareMap.get(DcMotor.class, "carousel");
        slideDrive = hardwareMap.get(DcMotor.class, "slide");
        intakeDrive = hardwareMap.get(DcMotor.class, "intake");

        /*
        All drives except the left rear drive are somehow hooked up in reverse, so we set their
        direction to reverse to account for it.
         */
        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        rightFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        leftRearDrive.setDirection(DcMotor.Direction.FORWARD);
        rightRearDrive.setDirection(DcMotor.Direction.REVERSE);
        carouselDrive.setDirection(DcMotor.Direction.FORWARD);

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {
            final double drive = -gamepad1.left_stick_y;
            final double strafe = gamepad1.left_stick_x;
            final double turn = gamepad1.right_stick_x;

            /*
            Using Mecanum wheel behaviors presented here:
            https://docs.revrobotics.com/15mm/ftc-starter-kit-mecanum-drivetrain/mecanum-wheel-setup-and-behavior
             */
            final double leftFrontPower = Range.clip(drive + strafe + turn, -1.0, 1.0);
            final double rightFrontPower = Range.clip(drive - strafe -  turn, -1.0, 1.0);
            final double leftRearPower = Range.clip(drive - strafe + turn, -1.0, 1.0);
            final double rightRearPower = Range.clip(drive + strafe - turn, -1.0, 1.0);

            final double carouselPower = ((gamepad2.triangle ? 1.0 : 0.0) - (gamepad2.cross ? 1.0 : 0.0)) * 0.6;
            final double slidePower = (gamepad2.right_trigger - gamepad2.left_trigger) * 0.4;
            final double intakePower = gamepad2.left_bumper || gamepad2.right_bumper ? 0.8 : 0.0;

            leftFrontDrive.setPower(leftFrontPower);
            rightFrontDrive.setPower(rightFrontPower);
            leftRearDrive.setPower(leftRearPower);
            rightRearDrive.setPower(rightRearPower);
            carouselDrive.setPower(carouselPower);
            slideDrive.setPower(slidePower);
            intakeDrive.setPower(intakePower);

            telemetry.addData("Status", "Run Time: " + runtime.toString());
            telemetry.addData("Motors",
                    "leftFront (%.2f), rightFront (%.2f), leftRear (%.2f), rightRear (%.2f), carousel (%.2f), slide (%.2f), intake (%.2f)",
                    leftFrontPower, rightFrontPower, leftRearPower, rightRearPower, carouselPower, slidePower, intakePower);
            telemetry.update();
        }
    }
}
