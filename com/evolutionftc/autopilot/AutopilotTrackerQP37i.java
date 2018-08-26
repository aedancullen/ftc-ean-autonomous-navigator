package com.evolutionftc.autopilot;


// Copyright (c) 2016-2018 Aedan Cullen and/or Evolution Robotics.


// QP37i tracking driver


import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

public class AutopilotTrackerQP37i extends AutopilotTracker{

	private DcMotor x;
	private DcMotor y;

	long xenc;
	long yenc;
	double ticksPerUnit;

	private BNO055IMU imu;

	int nSubsteps;

	private double[] robotPosition = new double[3];
	private double[] robotAttitude = new double[3];

	private double[] sensorPosRelativeToRobot = new double[3];


	private static double[][] matmul(double[][] firstarray, double[][] secondarray) {

		double [][] result = new double[firstarray.length][secondarray[0].length];

		for (int i = 0; i < firstarray.length; i++) {
			for (int j = 0; j < secondarray[0].length; j++) {
				for (int k = 0; k < firstarray[0].length; k++) {
					result[i][j] += firstarray[i][k] * secondarray[k][j];
				}
			}
		}

		return result;
	}

	private static double[][] buildTransform(double[] xyz, double[] hpr){

		// See http://planning.cs.uiuc.edu/node104.html

		double[][] out = new double[4][4];

		double x = xyz[0];
		double y = xyz[1];
		double z = xyz[2];
		double h = hpr[0];
		double p = hpr[1];
		double r = hpr[2];


		out[0][0] = Math.cos(h)*Math.cos(p);
		out[0][1] = (Math.cos(h)*Math.sin(p)*Math.sin(r)) - (Math.sin(h)*Math.cos(r));
		out[0][2] = (Math.cos(h)*Math.sin(p)*Math.cos(r)) + (Math.sin(h)*Math.sin(r));
		out[0][3] = x;
		out[1][0] = Math.sin(h)*Math.cos(p);
		out[1][1] = (Math.sin(h)*Math.sin(p)*Math.sin(r)) + (Math.cos(h)*Math.cos(r));
		out[1][2] = (Math.sin(h)*Math.sin(p)*Math.cos(r)) - (Math.cos(h)*Math.sin(r));
		out[1][3] = y;
		out[2][0] = -Math.sin(p);
		out[2][1] = Math.cos(p)*Math.sin(r);
		out[2][2] = Math.cos(p)*Math.cos(r);
		out[2][3] = z;
		out[3][0] = 0.0;
		out[3][1] = 0.0;
		out[3][2] = 0.0;
		out[3][3] = 1.0;

		return out;
	}

	private static double[] transform(double[] point, double[] translation, double[] rotation) {

		// Transform a 3D body by rotation and then translation.

		double[][] transform = buildTransform(point, rotation);

		double[][] params = {
			{translation[0]},
			{translation[1]},
			{translation[2]},
			{1.0}
		};

		double[][] result = matmul(transform, params);

		double[] out = new double[3];

		out[0] = result[0][0];
		out[1] = result[1][0];
		out[2] = result[2][0];

		return out;
	}


	public AutopilotTrackerQP37i(DcMotor x, DcMotor y, double[] sensorPosRelativeToRobot, double ticksPerUnit, BNO055IMU imu, int nSubsteps) {
		this.x = x;
		this.y = y;
		this.sensorPosRelativeToRobot = sensorPosRelativeToRobot;
		this.imu = imu;
		this.ticksPerUnit = ticksPerUnit;
		this.nSubsteps = nSubsteps;

		//BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
		//parameters.angleUnit           = BNO055IMU.AngleUnit.RADIANS;
		//parameters.calibrationDataFile = "BNO055IMUCalibration.json";

		//imu.initialize(parameters);
	}


	public void update() {

		double[] oldRobotAttitude = new double[3];
		oldRobotAttitude[0] = robotAttitude[0];
		oldRobotAttitude[1] = robotAttitude[1];
		oldRobotAttitude[2] = robotAttitude[2];

		Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS);
		robotAttitude[0] = angles.firstAngle;

		long ticksX = x.getCurrentPosition();
		long ticksY = y.getCurrentPosition();

		double xval = ((double)(ticksX - xenc) / ticksPerUnit);
		double yval = ((double)(ticksY - yenc) / ticksPerUnit);

		xenc = ticksX;
		yenc = ticksY;

		double[] translationDeltaPerStep = {xval / (double)nSubsteps, yval / (double)nSubsteps, 0.0};
		double[] rotationDeltaPerStep = new double[3];
		for (int i=0; i<3; i++) {
			// rotationDeltaPerStep * nSubsteps equals the difference between oldRobotAttitude and robotAttitude.
			rotationDeltaPerStep[i] = (robotAttitude[i] - oldRobotAttitude[i]) / (double)nSubsteps;
		}

		for (int i=0; i < nSubsteps; i++) {
			double[] robotAttitudeThisStep = new double[3];

			for (int j=0; j<3; j++) {
				// Probably the most confusing part. Add (i) mini-steps to the old starting attitude.
				robotAttitudeThisStep[j] = oldRobotAttitude[j] + (rotationDeltaPerStep[j] * i);
			}
			robotPosition = transform(robotPosition, translationDeltaPerStep, robotAttitudeThisStep);
		}

		// Sensor pos compensate

		for (int i=0; i<3; i++) {
			robotPosition[i] -= sensorPosRelativeToRobot[i];
		}

	}

	public double[] getRobotPosition() {
		return robotPosition;
	}

	public double[] getRobotAttitude() {
		return robotAttitude;
	}

	public void setRobotPosition(double[] position) {
		robotPosition = position;
	}

	public void setRobotAttitude(double[] attitude) {
		robotAttitude = attitude;
	}

}
