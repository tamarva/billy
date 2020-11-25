package com.billy.billy.text_recognition;

import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.components.Preconditions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.util.ArrayList;
import java.util.List;

public class BillLine {
    private static final String TAG = BillLine.class.getSimpleName();
    private static final int RANGE = 60;
    private int minY;
    private int maxY;
    private Rect lineFrame;
    private boolean isAdded;
    private List<FirebaseVisionText.Line> lines = new ArrayList<>();

    public BillLine(@NonNull FirebaseVisionText.Line line) {
        Preconditions.checkNotNull(line);
        lines.add(line);
        lineFrame = line.getBoundingBox();
        Preconditions.checkNotNull(lineFrame);
        minY = getMinY();
        maxY = getMaxY();
        isAdded = false;
    }

    public boolean isAdded() {
        return isAdded;
    }

    public void toggleAdded() {
        isAdded = !isAdded;
    }

    public List<FirebaseVisionText.Line> getLines(){
        return lines;
    }

    private int getMinY() {
        return lineFrame.bottom;
    }

    private int getMaxY() {
        return lineFrame.top;
    }

    public boolean checkYRange(@NonNull BillLine other) {
        Preconditions.checkNotNull(other);
        int maxYVal = Math.max(minY, other.minY);
        int minYVal = Math.min(maxY, other.maxY);
        int overlapYLength = maxYVal - minYVal;
        Log.d(TAG, "checkYRange: overlap " + overlapYLength);

        int length = maxY - minY;
        int otherLength = other.maxY - other.minY;
        int minLength = Math.min(length, otherLength);
        Log.d(TAG, "checkYRange: minLength" + minLength);

        if (overlapYLength >= 0.85 * minLength) {
            return true;
        }
        return false;
    }

    public boolean checkYCenterRange(@NonNull BillLine other) {
        Preconditions.checkNotNull(other);
        return lineFrame.exactCenterY() - RANGE < other.lineFrame.exactCenterY() &&
                other.lineFrame.exactCenterY() < lineFrame.exactCenterY() + RANGE;
    }

    public void addOther(@NonNull BillLine other) {
        Preconditions.checkNotNull(other);
        for (FirebaseVisionText.Line otherLine : other.lines) {
            Log.d(TAG, "addOther: line " + otherLine.getText());
            int pos = 0;

            while (pos < lines.size()) {
                int xPos = getLineXPos(pos);
                if (other.lineFrame.left < xPos)
                {
                    break;
                }
                ++pos;
            }

            if (pos == lines.size()) {
                lines.add(otherLine);
            } else {
                addBeforeIndex(pos, otherLine);
            }
        }

        updateFrame(other.lineFrame);
        minY = lineFrame.bottom;
        maxY = lineFrame.bottom;
    }

    private int getLineXPos(int pos) {
        FirebaseVisionText.Line currLine = lines.get(pos);
        Rect boundingBox = currLine.getBoundingBox();
        Preconditions.checkNotNull(boundingBox);
        return boundingBox.left;
    }

    private void addBeforeIndex(int index, @NonNull FirebaseVisionText.Line otherLine) {
        Preconditions.checkNotNull(otherLine);
        List<FirebaseVisionText.Line> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (i != index) {
                newLines.add(lines.get(i));
            } else {
                newLines.add(otherLine);
                newLines = addRestOfLines(i, newLines);
            }
        }
        lines = newLines;
    }

    private List<FirebaseVisionText.Line> addRestOfLines(int index, @NonNull List<FirebaseVisionText.Line> newLines) {
        for (int i = index; i < lines.size(); i++) {
            newLines.add(lines.get(i));
        }
        return newLines;
    }

    private void updateFrame(@NonNull Rect otherLineFrame) {
        Preconditions.checkNotNull(otherLineFrame);
        this.lineFrame.union(otherLineFrame);
    }
}
