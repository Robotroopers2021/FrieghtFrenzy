package org.firstinspires.ftc.teamcode


import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.hardware.rev.Rev2mDistanceSensor
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive
import org.firstinspires.ftc.teamcode.stateMachine.StateMachineBuilder
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence

@Autonomous(preselectTeleOp = "CompTeleOp")
class Fmylife : OpMode() {

    private var startPose = Pose2d(11.0, 57.25, Math.toRadians(90.0))

    private var motionTimer = ElapsedTime()

    private lateinit var outtakeServo: Servo

    private lateinit var intakeMotor: DcMotor

    private lateinit var distanceSensor : Rev2mDistanceSensor

    private val arm = Arm()

    private lateinit var InitialDepositTraj : TrajectorySequence

    private lateinit var CycleOneWarehouseTraj : TrajectorySequence

    private lateinit var CycleOneDepsoitTraj : TrajectorySequence

    private lateinit var CycleTwoWarehouseTraj : TrajectorySequence

    private lateinit var CycleTwoDepsoitTraj : TrajectorySequence

    private lateinit var ParkAtEnd :TrajectorySequence



    private fun moveOuttakeToOut(){
        outtakeServo.position = 0.60

    }

    private fun moveOuttakeToLock(){
        outtakeServo.position = 0.80
    }

    private fun moveOuttakeToOpen(){
        outtakeServo.position = 0.90

    }

    private fun intakeFreight(){
        intakeMotor.power = -1.0
    }

    private fun stopIntake(){
        intakeMotor.power = 0.0
    }

    private fun getFreightOut(){
        intakeMotor.power = 1.0
    }



    lateinit var drive: SampleMecanumDrive


    private enum class InitialDepositStates {
        INITIAL_DEPOSIT,
        CYCLE_ONE_WAREHOUSE,
        CYCLE_ONE_DEPOSIT,
        CYCLE_TWO_WAREHOUSE,
        CYCLE_TWO_DEPOSIT,
        PARK_AT_END,

    }

    private val initialDepositStateMachine = StateMachineBuilder<InitialDepositStates>()
        .state(InitialDepositStates.INITIAL_DEPOSIT)
        .onEnter {
            drive.followTrajectorySequenceAsync(InitialDepositTraj)
        }
        .transition{!drive.isBusy}

        .state(InitialDepositStates.CYCLE_ONE_WAREHOUSE)
        .onEnter{
            drive.followTrajectorySequenceAsync(CycleOneWarehouseTraj)
            arm.moveArmToBottomPos()
        }
        .transition{!drive.isBusy}

        .state(InitialDepositStates.CYCLE_ONE_DEPOSIT)
        .onEnter{
            drive.followTrajectorySequenceAsync(CycleOneDepsoitTraj)
            stopIntake()
        }
        .transition{!drive.isBusy}

        .state(InitialDepositStates.CYCLE_TWO_WAREHOUSE)
        .onEnter{
            drive.followTrajectorySequenceAsync(CycleTwoWarehouseTraj)
            arm.moveArmToBottomPos()
        }
        .transition{!drive.isBusy}

        .state(InitialDepositStates.CYCLE_TWO_DEPOSIT)
        .onEnter{
            drive.followTrajectorySequenceAsync(CycleTwoDepsoitTraj)
            stopIntake()
        }
        .transition{!drive.isBusy}

        .state(InitialDepositStates.PARK_AT_END)
        .onEnter{
            drive.followTrajectorySequenceAsync(ParkAtEnd)
            arm.moveArmToBottomPos()
        }
        .transition{!drive.isBusy}


        .build()




    override fun init() {
        drive = SampleMecanumDrive(hardwareMap)
        arm.init(hardwareMap)
        outtakeServo = hardwareMap.get(Servo::class.java, "Outtake") as Servo
        outtakeServo.position = 0.80
        intakeMotor = hardwareMap.dcMotor["Intake"]
        intakeMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        distanceSensor = hardwareMap.get(Rev2mDistanceSensor::class.java, "distanceSensor") as Rev2mDistanceSensor

        InitialDepositTraj = drive.trajectorySequenceBuilder(startPose)
            .setReversed(true)
            .splineToSplineHeading( Pose2d(5.0, 30.0, Math.toRadians(50.0)), Math.toRadians(220.0))
            .addTemporalMarker(0.2) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(1.5) {
                moveOuttakeToOut()
            }
            .lineToSplineHeading( Pose2d(-11.0, 45.0, Math.toRadians(90.0)))
            .build()

        CycleOneWarehouseTraj = drive.trajectorySequenceBuilder(Pose2d(-11.0, 45.0 , Math.toRadians(90.0)))
            .setReversed(false)
            .splineToSplineHeading(Pose2d(40.0, 65.75, Math.toRadians(0.0)), Math.toRadians(0.0))
            .splineToConstantHeading(Vector2d(42.5, 65.75), Math.toRadians(0.0))
            .addTemporalMarker(0.1) {
                moveOuttakeToOpen()
            }
            .addTemporalMarker(1.5) {
                intakeFreight()
            }
            .waitSeconds(1.0)
            .build()

        CycleOneDepsoitTraj = drive.trajectorySequenceBuilder(Pose2d(42.5, 65.75, Math.toRadians(0.0)))
            .setReversed(true)
            .addTemporalMarker(0.1) {
                moveOuttakeToLock()
            }
            .addTemporalMarker(1.0) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(2.0) {
                moveOuttakeToOut()
            }
            .splineToConstantHeading( Vector2d(40.0, 65.75), Math.toRadians(180.0))
            .splineToSplineHeading( Pose2d(-11.0, 45.0 , Math.toRadians(90.0)), Math.toRadians(270.0))
            .splineToConstantHeading( Vector2d(-11.0, 42.0 ), Math.toRadians(270.0))
            .setReversed(false)
            .splineToConstantHeading( Vector2d(-11.0, 45.0), Math.toRadians(90.0))
            .build()

        CycleTwoWarehouseTraj = drive.trajectorySequenceBuilder( Pose2d(-11.0, 45.0 , Math.toRadians(90.0)))
            .setReversed(false)
            .addTemporalMarker(0.1) {
                moveOuttakeToOpen()
            }
            .addTemporalMarker(1.5) {
                intakeFreight()
            }
            .splineToSplineHeading(Pose2d(40.0, 67.75, Math.toRadians(0.0)), Math.toRadians(0.0))
            .splineToConstantHeading(Vector2d(44.5, 67.75), Math.toRadians(0.0))
            .waitSeconds(1.0)
            .build()

        CycleTwoDepsoitTraj = drive.trajectorySequenceBuilder(Pose2d(44.5, 67.75, Math.toRadians(0.0)))
            .setReversed(true)
            .addTemporalMarker(0.1) {
                moveOuttakeToLock()
            }
            .addTemporalMarker(0.2) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(2.0) {
                moveOuttakeToOut()
            }
            .splineToConstantHeading( Vector2d(40.0, 67.75), Math.toRadians(180.0))
            .splineToSplineHeading( Pose2d(-11.0, 45.0 , Math.toRadians(90.0)), Math.toRadians(270.0))
            .build()

        ParkAtEnd = drive.trajectorySequenceBuilder( Pose2d(-11.0, 45.0 , Math.toRadians(90.0)))
            .setReversed(false)
            .splineToSplineHeading(Pose2d(40.0, 69.75, Math.toRadians(0.0)), Math.toRadians(0.0))
            .addTemporalMarker(0.1) {
                moveOuttakeToOpen()
            }
            .waitSeconds(1.0)
            .build()


        drive.poseEstimate = startPose


        initialDepositStateMachine.start()
    }



    override fun loop() {
        initialDepositStateMachine.update()
        drive.update()
        arm.update()
    }
}