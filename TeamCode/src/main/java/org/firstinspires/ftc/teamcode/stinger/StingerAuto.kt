package org.firstinspires.ftc.teamcode.stinger

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.hardware.rev.Rev2mDistanceSensor
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.teamcode.archived.teleop.Arm
import org.firstinspires.ftc.teamcode.drive.DriveConstants
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive
import org.firstinspires.ftc.teamcode.stateMachine.StateMachineBuilder
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence
import org.firstinspires.ftc.teamcode.vision.Pipeline

@Autonomous
class StingerAuto : OpMode() {
    private lateinit var initialDeposit : TrajectorySequence
    private lateinit var warehouseOne : TrajectorySequence
    private lateinit var depositOne : TrajectorySequence
    private lateinit var warehouseTwo : TrajectorySequence
    private lateinit var depositTwo : TrajectorySequence
    private lateinit var warehouseThree : TrajectorySequence
    private lateinit var depositThree : TrajectorySequence
    private lateinit var warehouseFour : TrajectorySequence
    private lateinit var depositFour : TrajectorySequence
    private lateinit var finalPark : TrajectorySequence

    val startPose = Pose2d(6.25, 63.0, Math.toRadians(90.0))

    private var value = 0.0

    private var motionTimer = ElapsedTime()

    private lateinit var distanceSensor : Rev2mDistanceSensor

    private enum class InitialDepositStates {
        INITIAL_DEPOSIT,
        WAREHOUSE_ONE,
        DEPOSIT_ONE,
        WAREHOUSE_TWO,
        DEPOSIT_TWO,
        WAREHOUSE_THREE,
        DEPOSIT_THREE,
        WAREHOUSE_FOUR,
        DEPOSIT_FOUR,
        FINAL_PARK
    }

    lateinit var drive: SampleMecanumDrive

    private val arm = Arm()

    private lateinit var outtakeServo: Servo

    private lateinit var intakeMotor: DcMotor

    private fun moveOuttakeToOut(){
        outtakeServo.position = 0.60

    }

    private fun moveOuttakeToLock(){
        outtakeServo.position = 0.78
    }

    private fun moveOuttakeToOpen(){
        outtakeServo.position = 0.92

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


    private val initialDepositStateMachine = StateMachineBuilder<InitialDepositStates>()
        .state(InitialDepositStates.INITIAL_DEPOSIT)
        .onEnter{
            drive.followTrajectorySequenceAsync(initialDeposit)
        }
        .onExit (motionTimer::reset)
        .transition{!drive.isBusy}

        .state(InitialDepositStates.WAREHOUSE_ONE)
        .onEnter{
            drive.followTrajectorySequenceAsync(warehouseOne)
        }
        .transition { !drive.isBusy || (value < 75.0 && motionTimer.seconds() > 2.0)}

        .state(InitialDepositStates.DEPOSIT_ONE)
        .onEnter{drive.followTrajectorySequenceAsync(depositOne)}
        .onExit (motionTimer::reset)
        .transition{!drive.isBusy}

        .state(InitialDepositStates.WAREHOUSE_TWO)
        .onEnter{
            drive.followTrajectorySequenceAsync(warehouseTwo)
        }
        .transition { !drive.isBusy || (value < 75.0 && motionTimer.seconds() > 2.0)}

        .state(InitialDepositStates.DEPOSIT_TWO)
        .onEnter{drive.followTrajectorySequenceAsync(depositTwo)}
        .onExit (motionTimer::reset)
        .transition{!drive.isBusy}

        .state(InitialDepositStates.WAREHOUSE_THREE)
        .onEnter{
            drive.followTrajectorySequenceAsync(warehouseThree)
        }
        .transition { !drive.isBusy || (value < 75.0 && motionTimer.seconds() > 2.0)}

        .state(InitialDepositStates.DEPOSIT_THREE)
        .onEnter{drive.followTrajectorySequenceAsync(depositThree)}
        .onExit (motionTimer::reset)
        .transition{!drive.isBusy}

        .state(InitialDepositStates.WAREHOUSE_FOUR)
        .onEnter{
            drive.followTrajectorySequenceAsync(warehouseFour)
        }
        .transition { !drive.isBusy || (value < 75.0 && motionTimer.seconds() > 2.0)}

        .state(InitialDepositStates.DEPOSIT_FOUR)
        .onEnter{drive.followTrajectorySequenceAsync(depositFour)}
        .onExit (motionTimer::reset)
        .transition{!drive.isBusy}

        .state(InitialDepositStates.FINAL_PARK)
        .onEnter{
            drive.followTrajectorySequenceAsync(finalPark)
        }
        .transition {!drive.isBusy}
        .build()




    override fun init() {
        drive = SampleMecanumDrive(hardwareMap)
        arm.init(hardwareMap)
        outtakeServo = hardwareMap.get(Servo::class.java, "Outtake") as Servo
        outtakeServo.position = 0.78
        intakeMotor = hardwareMap.dcMotor["Intake"]
        intakeMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        distanceSensor = hardwareMap.get(Rev2mDistanceSensor::class.java, "distanceSensor") as Rev2mDistanceSensor

        initialDeposit = drive.trajectorySequenceBuilder(startPose)
            .setReversed(true)
            //initial deposit
            .lineToLinearHeading( Pose2d(-5.0, 45.0, -Math.toRadians(295.0)),
                SampleMecanumDrive.getVelocityConstraint(45.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            .addTemporalMarker(0.2) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(1.0) {
                moveOuttakeToOut()
            }
            .build()

        warehouseOne = drive.trajectorySequenceBuilder(initialDeposit.end())
            .setReversed(false)
            //first half of warehouse one
            .splineToSplineHeading( Pose2d(17.0, 68.0, Math.toRadians(360.0)), -Math.toRadians(360.0))
            //second half of warehouse one
            .splineToConstantHeading( Vector2d(42.0, 68.0), Math.toRadians(0.0),
                SampleMecanumDrive.getVelocityConstraint(40.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(40.0))
            .addTemporalMarker(0.1) {
                moveOuttakeToLock()
            }
            .addTemporalMarker( 0.1) {
                arm.moveArmToBottomPos()
            }
            .addTemporalMarker(0.8) {
                moveOuttakeToOpen()
            }
            .addTemporalMarker(1.5) {
                intakeFreight()
            }
            .build()

        depositOne = drive.trajectorySequenceBuilder(warehouseOne.end())
            .setReversed(true)
            //exiting warehouse one
            .splineToConstantHeading( Vector2d(17.0, 68.0), -Math.toRadians(180.0),
                SampleMecanumDrive.getVelocityConstraint(DriveConstants.MAX_VEL, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            //deposit one
            .lineToLinearHeading(Pose2d(-2.0, 44.2, Math.toRadians(65.0)),
                SampleMecanumDrive.getVelocityConstraint(55.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            .addTemporalMarker(0.05) {
                moveOuttakeToLock()
                stopIntake()
            }
            .addTemporalMarker(0.1) {
                getFreightOut()
            }
            .addTemporalMarker(0.6) {
                stopIntake()
            }
            .addTemporalMarker(1.5) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(2.25) {
                moveOuttakeToOut()
            }
            .build()

        warehouseTwo = drive.trajectorySequenceBuilder(depositOne.end())
            .setReversed(false)
            //first half of warehouse one
            .splineToSplineHeading( Pose2d(17.0, 68.0, Math.toRadians(360.0)), -Math.toRadians(360.0))
            //second half of warehouse one
            .splineToConstantHeading( Vector2d(46.0, 68.0), Math.toRadians(0.0),
                SampleMecanumDrive.getVelocityConstraint(40.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(40.0))
            .addTemporalMarker(0.1) {
                moveOuttakeToLock()
            }
            .addTemporalMarker( 0.1) {
                arm.moveArmToBottomPos()
            }
            .addTemporalMarker(0.8) {
                moveOuttakeToOpen()
            }
            .addTemporalMarker(1.5) {
                intakeFreight()
            }
            .build()

        depositTwo = drive.trajectorySequenceBuilder(warehouseTwo.end())
            .setReversed(true)
            //exiting warehouse one
            .splineToConstantHeading( Vector2d(17.0, 68.0), -Math.toRadians(180.0),
                SampleMecanumDrive.getVelocityConstraint(DriveConstants.MAX_VEL, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            //deposit one
            .lineToLinearHeading(Pose2d(-2.0, 44.2, Math.toRadians(65.0)),
                SampleMecanumDrive.getVelocityConstraint(55.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            .addTemporalMarker(0.05) {
                moveOuttakeToLock()
                stopIntake()
            }
            .addTemporalMarker(0.1) {
                getFreightOut()
            }
            .addTemporalMarker(0.6) {
                stopIntake()
            }
            .addTemporalMarker(1.5) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(2.25) {
                moveOuttakeToOut()
            }
            .build()

        warehouseThree = drive.trajectorySequenceBuilder(depositTwo.end())
            .setReversed(false)
            //first half of warehouse one
            .splineToSplineHeading( Pose2d(17.0, 68.0, Math.toRadians(360.0)), -Math.toRadians(360.0))
            //second half of warehouse one
            .splineToConstantHeading( Vector2d(54.0, 68.0), Math.toRadians(0.0),
                SampleMecanumDrive.getVelocityConstraint(40.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(40.0))
            .addTemporalMarker(0.1) {
                moveOuttakeToLock()
            }
            .addTemporalMarker( 0.1) {
                arm.moveArmToBottomPos()
            }
            .addTemporalMarker(0.8) {
                moveOuttakeToOpen()
            }
            .addTemporalMarker(1.5) {
                intakeFreight()
            }
            .build()

        depositThree = drive.trajectorySequenceBuilder(warehouseThree.end())
            .setReversed(true)
            //exiting warehouse one
            .splineToConstantHeading( Vector2d(17.0, 68.0), -Math.toRadians(180.0),
                SampleMecanumDrive.getVelocityConstraint(DriveConstants.MAX_VEL, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            //deposit one
            .lineToLinearHeading(Pose2d(-2.0, 44.2, Math.toRadians(65.0)),
                SampleMecanumDrive.getVelocityConstraint(55.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            .addTemporalMarker(0.05) {
                moveOuttakeToLock()
                stopIntake()
            }
            .addTemporalMarker(0.1) {
                getFreightOut()
            }
            .addTemporalMarker(0.6) {
                stopIntake()
            }
            .addTemporalMarker(1.5) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(2.25) {
                moveOuttakeToOut()
            }
            .build()

        warehouseFour = drive.trajectorySequenceBuilder(depositThree.end())
            .setReversed(false)
            //first half of warehouse one
            .splineToSplineHeading( Pose2d(17.0, 68.0, Math.toRadians(360.0)), -Math.toRadians(360.0))
            //second half of warehouse one
            .splineToConstantHeading( Vector2d(56.0, 68.0), Math.toRadians(0.0),
                SampleMecanumDrive.getVelocityConstraint(40.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(40.0))
            .addTemporalMarker(0.1) {
                moveOuttakeToLock()
            }
            .addTemporalMarker( 0.1) {
                arm.moveArmToBottomPos()
            }
            .addTemporalMarker(0.8) {
                moveOuttakeToOpen()
            }
            .addTemporalMarker(1.5) {
                intakeFreight()
            }
            .build()

        depositFour = drive.trajectorySequenceBuilder(warehouseFour.end())
            .setReversed(true)
            //exiting warehouse one
            .splineToConstantHeading( Vector2d(17.0, 68.0), -Math.toRadians(180.0),
                SampleMecanumDrive.getVelocityConstraint(DriveConstants.MAX_VEL, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            //deposit one
            .lineToLinearHeading(Pose2d(-2.0, 44.2, Math.toRadians(65.0)),
                SampleMecanumDrive.getVelocityConstraint(55.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
            .addTemporalMarker(0.05) {
                moveOuttakeToLock()
                stopIntake()
            }
            .addTemporalMarker(0.1) {
                getFreightOut()
            }
            .addTemporalMarker(0.6) {
                stopIntake()
            }
            .addTemporalMarker(1.5) {
                arm.moveArmToTopPos()
            }
            .addTemporalMarker(2.25) {
                moveOuttakeToOut()
            }
            .build()

        finalPark = drive.trajectorySequenceBuilder(depositFour.end())
            .setReversed(false)
            //first half of warehouse one
            .splineToSplineHeading( Pose2d(17.0, 68.0, Math.toRadians(360.0)), -Math.toRadians(360.0))
            //second half of warehouse one
            .splineToConstantHeading( Vector2d(54.0, 68.0), Math.toRadians(0.0),
                SampleMecanumDrive.getVelocityConstraint(50.0, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                SampleMecanumDrive.getAccelerationConstraint(50.0))
            .addTemporalMarker(0.1) {
                moveOuttakeToLock()
            }
            .addTemporalMarker( 0.1) {
                arm.moveArmToBottomPos()
            }
            .addTemporalMarker(0.8) {
                moveOuttakeToOpen()
            }
            .addTemporalMarker(1.5) {
                intakeFreight()
            }
            .build()

        drive.poseEstimate = startPose
    }

    override fun start() {
        super.start()
        initialDepositStateMachine.start()
    }

    override fun loop() {
        initialDepositStateMachine.update()
        value = distanceSensor.getDistance(DistanceUnit.MM)
        drive.update()
        arm.update()
    }
}