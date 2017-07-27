package com.evolutionftc.autopilot;



import org.firstinspires.ftc.robotcore.external.Telemetry;



// this is a snazzy autopilot for ftc made by aedan.
// it was made somewhat for team #9867, 'evolution'
// it uses lots of maths.
// it is gpl licensed.
// copyright 2017 aedan cullen.


public class AutopilotHost {

    Telemetry telemetry;

    public enum NavigationStatus {RUNNING, STOPPED};

    private NavigationStatus navigationStatus = NavigationStatus.STOPPED;

    private double basePower;
    private double lowestPower;
    private double powerGain;

    private double navigationHalfway;
    private boolean rampUp;
    private boolean rampDown;

    private double[] navigationTarget = new double[3];
    private double steeringGain;
    private double[] accuracyThreshold;

    private double[] robotAttitude = new double[3];

    private double[] robotPosition = new double[3];

    public AutopilotHost(Telemetry telemetry) {
        this.telemetry = telemetry;
	telemetryUpdate();
    }

    private void telemetryUpdate() {
        telemetry.addData("* AutopilotHost", "\n" +
                "\t nav: " + navigationStatus.toString().toLowerCase() + "\n" +
                "\t trg: " + round(navigationTarget[0]) + "," + round(navigationTarget[1]) + "," + round(navigationTarget[2]) + "\n" +
                "\t pos: " + round(robotPosition[0]) + "," + round(robotPosition[1]) + "," + round(robotPosition[2]) + "\n" +
                "\t att: " + round(robotAttitude[0]) + "," + round(robotAttitude[1]) + "," + round(robotAttitude[2]));
    }
	
    public void communicate(AutopilotTracker tracker) {
        tracker.setRobotPosition(robotPosition);
    	robotAttitude = tracker.getRobotAttitude();
        robotPosition = tracker.getRobotPosition();
    }

    public NavigationStatus getNavigationStatus() {
        return navigationStatus;
    }
	
    public void setNavigationStatus(NavigationStatus navigationStatus) {
	this.navigationStatus = navigationStatus;
	telemetryUpdate();
    }

    public void setNavigationTarget(AutopilotSegment target) {
        setNavigationTarget(target.navigationTarget, target.steeringGain, target.accuracyThreshold, target.basePower, target.lowestPower, target.powerGain, target.rampUp, target.rampDown);
    }

    public void setNavigationTarget(double[] navigationTarget, double steeringGain, double[] accuracyThreshold, double basePower, double lowestPower, double powerGain, boolean rampUp, boolean rampDown) {
        this.navigationTarget = navigationTarget;
        this.steeringGain = steeringGain;
        this.accuracyThreshold = accuracyThreshold;
        this.basePower = basePower;
        this.lowestPower = lowestPower;
        this.powerGain = powerGain;
        this.rampUp = rampUp;
        this.rampDown = rampDown;
        double distX = navigationTarget[0] - robotPosition[0];
        double distY = navigationTarget[1] - robotPosition[1];
        double dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
        navigationHalfway = dist / 2;
	telemetryUpdate();
    }

    public double[] getNavigationTarget() {
        return navigationTarget;
    }

    public double[] getRobotAttitude() {
        return robotAttitude;
    }

    public double[] getRobotPosition() {
        return robotPosition;
    }

    public void setRobotPosition(double[] position) {
        robotPosition = position;
    }
	
    private static boolean hasReached(double param1, double param2, double threshold) {
        return (Math.abs(param2 - param1) < threshold);
    }

    private static double round(double in) {
        double roundOff = (double) Math.round(in * 100) / 100;
        return roundOff;
    }

    public double[] navigationTickDifferential() {

        telemetryUpdate();
		
        if (
                    hasReached(robotPosition[0], navigationTarget[0], accuracyThreshold[0]) &&
                    hasReached(robotPosition[1], navigationTarget[1], accuracyThreshold[1]) &&
                    hasReached(robotPosition[2], navigationTarget[2], accuracyThreshold[2])
           )
        {
            navigationStatus = NavigationStatus.STOPPED;
            return new double[2];
        }
        else if (navigationStatus == NavigationStatus.RUNNING) {
            double distX = navigationTarget[0] - robotPosition[0];
            double distY = navigationTarget[1] - robotPosition[1];
            double dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
            double powerAdj = (dist - navigationHalfway) * powerGain;
            if (powerAdj > 0 && !rampUp) {
                powerAdj = 0;
            }
            if (powerAdj < 0 && !rampDown) {
                powerAdj = 0;
            }
            else if (powerAdj < 0 && rampDown) {
                powerAdj *= -1;
            }
            double angle = (Math.atan(distY / distX) - Math.PI / 2) - robotAttitude[3];
            if (angle > Math.PI) {
                angle = -angle - Math.PI;
            }
            if (Math.abs(angle) < Math.PI / 2) { // Drive forward
                double powerLeft = Math.max((basePower - powerAdj), lowestPower) - (angle * steeringGain);
                double powerRight = Math.max((basePower - powerAdj), lowestPower) + (angle * steeringGain);
                powerLeft = Math.min(powerLeft, 1);
                powerRight = Math.min(powerRight, 1);
                return new double[]{powerLeft, powerRight};
            }
            else {
                // Calculate the angle with respect to the back of the robot.
                if (angle > 0) {
                    angle = Math.PI - angle;
                }
                else{
                    angle = -Math.PI - angle;
                }
                // Drive backward
                double powerLeft = Math.max((-basePower - powerAdj), lowestPower) - (angle * steeringGain);
                double powerRight = Math.max((-basePower - powerAdj), lowestPower) + (angle * steeringGain);
                powerLeft = Math.min(powerLeft, 1);
                powerRight = Math.min(powerRight, 1);
                return new double[]{powerLeft, powerRight};
            }
        }

        else {
            return new double[2];
        }

    }

}
