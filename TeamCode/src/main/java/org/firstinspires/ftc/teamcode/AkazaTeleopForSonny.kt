package org.firstinspires.ftc.teamcode

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import org.firstinspires.ftc.teamcode.util.math.MathUtil
import kotlin.math.cos

@Config
@TeleOp
class AkazaTeleopForSonny : OpMode() {
    lateinit var fl: DcMotor
    lateinit var fr: DcMotor
    lateinit var bl: DcMotor
    lateinit var br: DcMotor

    lateinit var intakeMotor: DcMotor

    lateinit var duck: DcMotor

    lateinit var arm: DcMotor
    lateinit var outtakeServo: Servo

    var drive = 0.0
    var strafe = 0.0
    var rotate = 0.0
    var duckPower = 0.75

    var armController = PIDFController(PIDCoefficients(kp, ki, kd))

    var degreesPerTick = 90 / 184.0
    var ticksPerDegree = 184.0 / 90
    var targetTicks = 0.0
    var output = 0.0
    var pidOutput = 0.0
    var feedForward = 0.0

    private fun moveArmToDegree(degrees: Double) {
        targetAngle = degrees
        targetTicks = targetAngle * ticksPerDegree
        armController.reset()
        armController.targetPosition = targetTicks
    }

    private fun getFeedForward(targetAngle: Double): Double {
        return cos(targetAngle) * kcos
    }

    val kcosup = 0.5
    val kcosdown = 0.5

    fun feedforward(target: Double, up: Boolean): Double {
        return cos(target) * if(up) {
            kcosup
        } else {
            kcosdown
        }
    }
    private fun armControl() {
        when {
            gamepad1.left_bumper -> {
                moveArmToDegree(depositAngle)
            }
            gamepad1.right_bumper -> {
                moveArmToDegree(restAngle)
                outtakeServo.position = 0.92
            }
            gamepad1.a -> {
                moveArmToDegree(sharedAngle)
            }
            gamepad1.x -> {
                moveArmToDegree(sharedAngleAlliance)
            }
            gamepad1.b -> {
                moveArmToDegree(sharedAngleEnemy)
            }
        }


        val currentPosition = (arm.currentPosition - 114).toDouble()

        feedForward = getFeedForward(Math.toRadians(targetAngle))
        pidOutput = armController.update(currentPosition)
        output = feedForward + pidOutput

        arm.power = output


        val packet = TelemetryPacket()
        packet.put("target angle", targetAngle)
        packet.put("output", output)
        packet.put("Current Position", currentPosition)
        packet.put("degrees", currentPosition * degreesPerTick)
        packet.put("feedforward", getFeedForward(Math.toRadians(targetAngle)))
        packet.put("target ticks", targetTicks)
        FtcDashboard.getInstance().sendTelemetryPacket(packet)
    }

    private fun driveControl() {
        drive = MathUtil.cubicScaling(0.85, -gamepad1.left_stick_y.toDouble()) * 0.75
        strafe = MathUtil.cubicScaling(0.85, gamepad1.left_stick_x.toDouble()) * 0.75
        rotate = MathUtil.cubicScaling(0.85, gamepad1.right_stick_x.toDouble()) * 0.6
        fl.power = drive + strafe + rotate
        fr.power = drive - strafe - rotate
        bl.power = drive - strafe + rotate
        br.power = drive + strafe - rotate
    }

    private fun intakeControl() {



        if(gamepad1.right_trigger > 0.5) {
            intakeMotor.power = 1.0
        }

        else if(gamepad1.left_trigger > 0.5) {
            intakeMotor.power = -1.0
        } else {
            intakeMotor.power = 0.0
        }
    }

    private fun outtakeControl() {
        if (gamepad2.a) {
            outtakeServo.position = 0.92
        }
        if (gamepad2.b) {
            outtakeServo.position = 0.6
        }
        if (gamepad2.x) {
            outtakeServo.position = 0.83
        }
    }

    private fun duckControl() {
        if (gamepad1.dpad_left) {
            duck.power = duckPower
        } else if (gamepad1.dpad_right) {
            duck.power = -duckPower
        } else {
            duck.power = 0.0
        }
    }

    fun cubicScaling(k: Double, x: Double): Double {
        return (1 - k) * x + k * x * x * x
    }

    override fun init() {
        //Connect Motor
        fl = hardwareMap.get(DcMotor::class.java, "FL")
        fr = hardwareMap.get(DcMotor::class.java, "FR")
        bl = hardwareMap.get(DcMotor::class.java, "BL")
        br = hardwareMap.get(DcMotor::class.java, "BR")
        arm = hardwareMap.dcMotor["Arm"]
        duck = hardwareMap.get(DcMotor::class.java, "DuckL")
        intakeMotor = hardwareMap.dcMotor["Intake"]
        intakeMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        outtakeServo = hardwareMap.get(Servo::class.java, "Outtake") as Servo

        fl.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        fr.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        bl.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        br.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

        fl.direction = DcMotorSimple.Direction.REVERSE
        bl.direction = DcMotorSimple.Direction.REVERSE

        arm.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        arm.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        arm.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT

        outtakeServo.position = 0.9
        armController.reset()
        targetAngle = restAngle
        targetTicks = restAngle * ticksPerDegree
        armController.targetPosition = targetTicks
        telemetry.addData("STATUS", "Initialized")
        telemetry.update()
    }



    override fun loop() {
        driveControl()
        armControl()
        intakeControl()
        outtakeControl()
        duckControl()

        fun getFeedForward(targetAngle: Double): Double {
            return Math.cos(targetAngle) * kcos
        }

        val kcosup = 0.5
        val kcosdown = 0.5

        fun feedforward(target: Double, up: Boolean): Double {
            return Math.cos(target) * if(up) {
                kcosup
            } else {
                kcosdown
            }
        }



    }

    companion object {
        @JvmStatic var kp = 0.015
        @JvmStatic var ki = 0.0
        @JvmStatic var kd = 0.00075
        @JvmStatic var targetAngle = 0.0
        @JvmStatic var kcos = 0.275
        @JvmStatic var kv = 0.0
        @JvmStatic var depositAngle = 97.0
        @JvmStatic var restAngle = -55.0
        @JvmStatic var sharedAngle = 172.0
        @JvmStatic var sharedAngleAlliance = 174.0
        @JvmStatic var sharedAngleEnemy = 169.0
    }
}