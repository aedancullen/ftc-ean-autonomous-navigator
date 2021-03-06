package com.evolutionftc.autopilot;


import android.content.Context;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

// Copyright (c) 2016-2019 Aedan Cullen and/or Evolution Robotics.


public class AutopilotPath {

    private Telemetry telemetry;
    private Context appContext;

    String pathName;

    private List<AutopilotSegment> pathSegments = new ArrayList<AutopilotSegment>();
    private String currentSegmentId = "__init__";

    private String successSegmentId = "__init__";
    private String failSegmentId = "__init__";

    public AutopilotPath(String pathName, Telemetry telemetry, Context appContext) {
        this.telemetry = telemetry;
        this.appContext = appContext;
        this.pathName = pathName;

        InputStream ins = appContext.getResources().openRawResource(
        appContext.getResources().getIdentifier(pathName, "raw", appContext.getPackageName()));

        try (BufferedReader pathReader = new BufferedReader(new InputStreamReader(ins))) {
            String header = pathReader.readLine();
            String line = pathReader.readLine();
            if (!header.toLowerCase().equals(
            "id,success,fail,targetx,targety,targeth,xygain,hgain,xymax,xymin,hmax,useh")){
                throw new UnsupportedOperationException("Header line in CSV indicates file unparseable, is it of the correct format?");
            }
            while (line != null) {
                String[] lineSegments = line.split(",");
                AutopilotSegment newSegment = new AutopilotSegment();
                newSegment.id = lineSegments[0];
                newSegment.success = lineSegments[1];
                newSegment.fail = lineSegments[2];
                newSegment.navigationTarget = new double[] {
                    Double.valueOf(lineSegments[3]),
                    Double.valueOf(lineSegments[4]),
                    0.0
                };
                newSegment.orientationTarget = Double.valueOf(lineSegments[5]);

                newSegment.navigationGain = Double.valueOf(lineSegments[6]);
                newSegment.orientationGain = Double.valueOf(lineSegments[7]);

                newSegment.navigationMax = Double.valueOf(lineSegments[8]);
                newSegment.navigationMin = Double.valueOf(lineSegments[9]);

                newSegment.orientationMax = Double.valueOf(lineSegments[10]);
                newSegment.useOrientation = Boolean.valueOf(lineSegments[11]);
                
                pathSegments.add(newSegment);
                line = pathReader.readLine();
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Error loading path file: " + e.getMessage());
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new UnsupportedOperationException("Encountered unparseable line in path file, is it of the correct format?");
        }
        this.pathName = this.pathName + " - loaded " + pathSegments.size() + " segments";

    }

    public void telemetryUpdate() {
        telemetry.addData("* AutopilotPath", "\n" +
        "\t file:  " + pathName + "\n" +
        "\t current:  " + currentSegmentId + "\n" +
        "\t next:  " + successSegmentId + "\n" +
        "\t fallback:  " + failSegmentId);
        //telemetry.update();
    }

    public AutopilotSegment getSegment(String id) {
        for (AutopilotSegment segment : pathSegments) {
            if (segment.id.equals(id)) {
                return segment;
            }
        }
        return null;
    }

    public AutopilotSegment moveOnSuccess() {

        if (currentSegmentId.equals("__init__")) {
            currentSegmentId = "__start__";
            AutopilotSegment newCurrent = getSegment(currentSegmentId);
            successSegmentId = newCurrent.success;
            failSegmentId = newCurrent.fail;
            return newCurrent;
        }
        AutopilotSegment newCurrent = getSegment(successSegmentId);
        if (newCurrent != null) {
            currentSegmentId = successSegmentId;
            successSegmentId = newCurrent.success;
            failSegmentId = newCurrent.fail;
            return newCurrent;
        }

        return null;
    }

    public AutopilotSegment moveOnFailure() {

        if (currentSegmentId.equals("__init__")) {
            currentSegmentId = "__start__";
            AutopilotSegment newCurrent = getSegment(currentSegmentId);
            successSegmentId = newCurrent.success;
            failSegmentId = newCurrent.fail;
            return newCurrent;
        }
        AutopilotSegment newCurrent = getSegment(failSegmentId);
        if (newCurrent != null) {
            currentSegmentId = failSegmentId;
            successSegmentId = newCurrent.success;
            failSegmentId = newCurrent.fail;
            return newCurrent;
        }

        return null;
    }

}
