package com.astoev.cave.survey.test.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.astoev.cave.survey.service.orientation.MeasurementsFilter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;


public class MeasurementsFilterTest {

    @Test
    public void testAveragingDisabled() {
        // filter without averaging
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(false);
        assertFalse(filter.isReady());

        // value returned as is
        float value1 = 1.23F;
        filter.startAveraging();
        filter.addMeasurement(value1);
        assertTrue(filter.isReady());
        assertEquals(value1, filter.getValue(), 0.001);
        assertEquals( "", filter.getAccuracyString());

        // last value always used
        float value2 = 2.34F;
        filter.addMeasurement(value2);
        assertTrue(filter.isReady());
        assertEquals(value2, filter.getValue(), 0.001);
        assertEquals( "", filter.getAccuracyString());

        // averaging 1 out of 1
        filter.startAveraging();
        filter.addMeasurement(value1);
        assertTrue(filter.isReady());
        assertEquals(value1, filter.getValue(), 0.001);
        assertEquals( "", filter.getAccuracyString());
    }

    @Test
    public void testAveragingEnabled() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(3);
        filter.startAveraging();

        filter.addMeasurement(1f);
        filter.addMeasurement(1f);
        filter.addMeasurement(2f);
        filter.addMeasurement(1f);

        assertTrue(filter.isReady());
        assertThat(filter.getMeasurements(), is(Arrays.asList(1f, 2f, 1f)));
        assertEquals(1.33, filter.getValue(), 0.01f);
        assertEquals( " ±0.47/3", filter.getAccuracyString());
    }

    @Test
    public void testPlusMinus() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(3);
        filter.startAveraging();

        filter.addMeasurement(-4f);
        filter.addMeasurement(5f);
        filter.addMeasurement(2f);
        filter.addMeasurement(-1f);
        filter.addMeasurement(10f);

        assertTrue(filter.isReady());
        assertThat(filter.getMeasurements(), is(Arrays.asList(5f, 2f, -1f)));
        assertEquals(2, filter.getValue(), 0);
        assertEquals( " ±2.45/3", filter.getAccuracyString());
    }

    @Test
    public void test360Degrees() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(2);
        filter.startAveraging();

        filter.addMeasurement(355f);
        filter.addMeasurement(15f);
        assertThat(filter.getMeasurements(), is(Arrays.asList(355f, 15f)));
        assertEquals(5f, filter.getValue(), 0.0f);

        filter.resetMeasurements();
        filter.addMeasurement(45f);
        filter.addMeasurement(47f);
        assertThat(filter.getMeasurements(), is(Arrays.asList(45f, 47f)));
        assertEquals(46f, filter.getValue(), 0.0f);

        filter.resetMeasurements();
        filter.addMeasurement(89f);
        filter.addMeasurement(91f);
        assertThat(filter.getMeasurements(), is(Arrays.asList(89f, 91f)));
        assertEquals(90f, filter.getValue(), 0.0f);

        filter.resetMeasurements();
        filter.addMeasurement(145f);
        filter.addMeasurement(147f);
        assertEquals(146f, filter.getValue(), 0.0f);

        filter.resetMeasurements();
        filter.addMeasurement(229f);
        filter.addMeasurement(231f);
        assertEquals(230f, filter.getValue(), 0.0f);

        filter.resetMeasurements();
        filter.addMeasurement(355f);
        filter.addMeasurement(5f);
        assertEquals(0f, filter.getValue(), 0.0f);
    }

    @Test
    public void testMultiple() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.startAveraging();
        filter.setNumMeasurements(5);

        filter.addMeasurement(-2f);
        filter.addMeasurement(1f);
        filter.addMeasurement(2f);
        filter.addMeasurement(3f);
        filter.addMeasurement(4f);
        filter.addMeasurement(3f);

        assertTrue(filter.isReady());
        assertThat(filter.getMeasurements(), is(Arrays.asList(-2f, 1f, 2f, 3f, 4f)));
        assertEquals(5, filter.getMeasurements().size());
        assertEquals(2.5f, filter.getValue(), 0.0f);
        assertEquals( " ±2.06/5", filter.getAccuracyString());
    }

    @Test
    public void testTwoAzimuthsAverage() {
        MeasurementsFilter filter = new MeasurementsFilter();
        assertEquals(2f, MeasurementsFilter.getAverageAzimuthDegrees(2f, 2f), 0.00f);
        assertEquals(10f, MeasurementsFilter.getAverageAzimuthDegrees(0f, 20f), 0.00f);
        assertEquals(1.5f, MeasurementsFilter.getAverageAzimuthDegrees(1f, 2f), 0.00f);
        assertEquals(330f, MeasurementsFilter.getAverageAzimuthDegrees(320f, 340f), 0.00f);
        assertEquals(110f, MeasurementsFilter.getAverageAzimuthDegrees(120f, 100f), 0.00f);
        assertEquals(110f, MeasurementsFilter.getAverageAzimuthDegrees(120f, 100f), 0.00f);
        assertEquals(0f, MeasurementsFilter.getAverageAzimuthDegrees(355f, 5f), 0.00f);
        assertEquals(356f, MeasurementsFilter.getAverageAzimuthDegrees(350f, 2f), 0.00f);
        assertEquals(1f, MeasurementsFilter.getAverageAzimuthDegrees(355f, 7f), 0.00f);
    }

    @Test
    public void testMultipleAzimuthsAverage() {
        MeasurementsFilter filter = new MeasurementsFilter();
        assertEquals(2f, filter.getAverageAzimuthDegrees(Arrays.asList(2f)), 0.00f);
        assertEquals(2f, filter.getAverageAzimuthDegrees(Arrays.asList(2f, 2f)), 0.00f);
        assertEquals(2f, filter.getAverageAzimuthDegrees(Arrays.asList(2f, 2f, 2f)), 0.00f);
        assertEquals(2f, filter.getAverageAzimuthDegrees(Arrays.asList(2f, 2f, 2f, 2f)), 0.00f);
        assertEquals(2.5f, filter.getAverageAzimuthDegrees(Arrays.asList(1f, 2f, 3f, 4f)), 0.00f);
        assertEquals(0f, filter.getAverageAzimuthDegrees(Arrays.asList(350f, 0f, 10f)), 0.00f);
        assertEquals(0f, filter.getAverageAzimuthDegrees(Arrays.asList(350f, 355f, 0f, 5f, 10f)), 0.00f);
        assertEquals(130f, filter.getAverageAzimuthDegrees(Arrays.asList(120f, 130f, 140f)), 0.00f);
        assertEquals(2.66f, filter.getAverageAzimuthDegrees(Arrays.asList(2f, 2f, 4f)), 0.01f);
        assertEquals(3.33f, filter.getAverageAzimuthDegrees(Arrays.asList(2f, 2f, 6f)), 0.01f);
        assertEquals(2f, filter.getAverageAzimuthDegrees(Arrays.asList(1f, 1f, 4f)), 0.00f);
        assertEquals(1.7f, filter.getAverageAzimuthDegrees(Arrays.asList(1.5f, 2f, 2.1f, 1.2f, 1.7f)), 0.00f);
        assertEquals(230f, filter.getAverageAzimuthDegrees(Arrays.asList(229f, 231f)), 0.00f);
        assertEquals(170f, filter.getAverageAzimuthDegrees(Arrays.asList(150f, 190f)), 0.00f);
        assertEquals(195f, filter.getAverageAzimuthDegrees(Arrays.asList(185f, 205f)), 0.00f);
        assertEquals(176f, filter.getAverageAzimuthDegrees(Arrays.asList(175f, 177f)), 0.00f);
    }

    @Test
    public void testNoiseReduction() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setNumMeasurements(10);
        float avg = 1f;
        float dev = 1f;
        // fewer values unchanged
        assertThat(filter.removeNoise(Arrays.asList(1f), avg, dev), is(Arrays.asList(1f)));
        assertThat(filter.removeNoise(Arrays.asList(1f, 1f, 1f, 1f, 1f), avg, dev), is(Arrays.asList(1f, 1f, 1f, 1f, 1f)));
        assertThat(filter.removeNoise(Arrays.asList(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f), avg, dev),
                is(Arrays.asList(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)));
        assertThat(filter.removeNoise(Arrays.asList(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f), avg, dev),
                is(Arrays.asList(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)));
        // distant values removed
        assertThat(filter.removeNoise(Arrays.asList(4f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f), avg, dev),
                is(Arrays.asList(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)));
        // close values kept
        assertThat(filter.removeNoise(Arrays.asList(2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f), avg, dev),
                is(Arrays.asList(2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)));
        // only most extreme removed
        assertThat(filter.removeNoise(Arrays.asList(1f, 2f, 6f, 1f, 8f, 1f, 1f, 4f, 1f, 1f), avg, dev),
                is(Arrays.asList(1f, 2f, 1f, 1f, 1f, 4f, 1f, 1f)));
        assertThat(filter.removeNoise(Arrays.asList(4f, 1f, -5f, 1f, 3f, 1f, 1f, 1f, 1f, 1f, 1f), avg, dev),
                is(Arrays.asList(1f, 1f, 3f, 1f, 1f, 1f, 1f, 1f, 1f)));
    }


    @Test
    public void testGetHalfDistance() {
        MeasurementsFilter filter = new MeasurementsFilter();
        assertEquals(1, MeasurementsFilter.getHalfDistance(0, 2), 0.0f);
        assertEquals(0.5, MeasurementsFilter.getHalfDistance(1, 2), 0.0f);
        assertEquals(2, MeasurementsFilter.getHalfDistance(350, 354), 0.0f);
        assertEquals(5, MeasurementsFilter.getHalfDistance(355, 5), 0.0f);
    }

    @Test
    public void testContinueSameDirection() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(3);
        filter.addMeasurement(2f);
        filter.addMeasurement(1f);
        filter.addMeasurement(1.5f);

        // sd going down
        filter.startAveraging();
        filter.addMeasurement(1.4f);
        assertFalse(filter.isReady());
        // and down
        filter.addMeasurement(1.4f);
        assertFalse(filter.isReady());
        // sd going up
        filter.addMeasurement(2f);
        assertTrue(filter.isReady());
        assertThat(filter.getMeasurements(), is(Arrays.asList(1.5f, 1.4f, 1.4f)));
        assertEquals(1.43f, filter.getValue(), 0.01f);
        assertEquals( " ±0.05/3", filter.getAccuracyString());
    }


    @Test
    public void testStopOnNoise() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(3);
        filter.startAveraging();

        filter.addMeasurement(1f);
        filter.addMeasurement(2f);
        filter.addMeasurement(2f);
        filter.addMeasurement(4f);

        assertTrue(filter.isReady());
        assertThat(filter.getMeasurements(), is(Arrays.asList(1f, 2f, 2f)));
        assertEquals(1.66f, filter.getValue(), 0.01f);
        assertEquals( " ±0.47/3", filter.getAccuracyString());
    }

    @Test
    public void testFindBiggestDistance() {
        // single
        assertEquals(4f, MeasurementsFilter.findMostDistantValue(Arrays.asList(1f, 2f, 1f, 4f, 2f), 2f), 0f);
        // multiple
        assertEquals(8f, MeasurementsFilter.findMostDistantValue(Arrays.asList(1f, 2f, 6f, 1f, 8f, 1f, 1f, 4f, 1f, 1f), 2f), 0f);
        assertEquals(6f, MeasurementsFilter.findMostDistantValue(Arrays.asList(1f, 2f, 6f, 1f, 6f, 1f, 4f, 1f, 1f), 2f), 0f);
        // leading
        assertEquals(9f, MeasurementsFilter.findMostDistantValue(Arrays.asList(9f, 1f, 2f, 6f, 1f, 1f, 1f, 4f, 1f, 1f), 2f), 0f);
        // trailing
        assertEquals(7f, MeasurementsFilter.findMostDistantValue(Arrays.asList(1f, 2f, 6f, 1f, 1f, 1f, 4f, 1f, 1f, 7f), 2f), 0f);
    }

    @Test
    public void testStandardDeviation() {
        // no deviation single value
        assertEquals(0f, MeasurementsFilter.getStandardDeviation(Arrays.asList(1f)), 0.00f);
        // no deviation same values
        assertEquals(0f, MeasurementsFilter.getStandardDeviation(Arrays.asList(1f, 1f)), 0.00f);
        // distance
        assertEquals(0.5f, MeasurementsFilter.getStandardDeviation(Arrays.asList(1f, 2f)), 0.00f);
        // going down
        assertEquals(0.47f, MeasurementsFilter.getStandardDeviation(Arrays.asList(1f, 1f, 2f)), 0.01f);
        // and down
        assertEquals(0.43f, MeasurementsFilter.getStandardDeviation(Arrays.asList(1f, 1f, 2f, 1f)), 0.01f);
        // around
        assertEquals(0.5f, MeasurementsFilter.getStandardDeviation(Arrays.asList(50f, 51f, 50f, 51f)), 0.01f);
        assertEquals(0.5f, MeasurementsFilter.getStandardDeviation(Arrays.asList(120f, 121f, 120f, 121f)), 0.01f);
        assertEquals(0.5f, MeasurementsFilter.getStandardDeviation(Arrays.asList(160f, 161f, 160f, 161f)), 0.01f);
        assertEquals(0.5f, MeasurementsFilter.getStandardDeviation(Arrays.asList(220f, 221f, 220f, 221f)), 0.01f);
        assertEquals(0.5f, MeasurementsFilter.getStandardDeviation(Arrays.asList(280f, 281f, 280f, 281f)), 0.01f);
        assertEquals(0.5f, MeasurementsFilter.getStandardDeviation(Arrays.asList(350f, 351f, 350f, 351f)), 0.01f);
        assertEquals(1f, MeasurementsFilter.getStandardDeviation(Arrays.asList(359f, 1f, 359f, 1f)), 0.01f);
    }

    @Test
    public void testFirstQuadrant() {
        assertTrue(MeasurementsFilter.isFirstQuadrant(0));
        assertTrue(MeasurementsFilter.isFirstQuadrant(10));
        assertTrue(MeasurementsFilter.isFirstQuadrant(40));
        assertTrue(MeasurementsFilter.isFirstQuadrant(50));
        assertTrue(MeasurementsFilter.isFirstQuadrant(80));
        assertTrue(MeasurementsFilter.isFirstQuadrant(90));
        assertFalse(MeasurementsFilter.isFirstQuadrant(91));
        assertFalse(MeasurementsFilter.isFirstQuadrant(120));
        assertFalse(MeasurementsFilter.isFirstQuadrant(220));
        assertFalse(MeasurementsFilter.isFirstQuadrant(300));
        assertFalse(MeasurementsFilter.isFirstQuadrant(350));
    }

    @Test
    public void testForthQuadrant() {
        assertTrue(MeasurementsFilter.isForthQuadrant(359));
        assertTrue(MeasurementsFilter.isForthQuadrant(300));
        assertTrue(MeasurementsFilter.isForthQuadrant(270));
        assertFalse(MeasurementsFilter.isForthQuadrant(200));
        assertFalse(MeasurementsFilter.isForthQuadrant(150));
        assertFalse(MeasurementsFilter.isForthQuadrant(110));
        assertFalse(MeasurementsFilter.isForthQuadrant(80));
        assertFalse(MeasurementsFilter.isForthQuadrant(20));
    }

    @Test
    public void testNeedNormalization() {
        assertFalse(MeasurementsFilter.needNormalization(Arrays.asList(1f, 2f, 3f)));
        assertFalse(MeasurementsFilter.needNormalization(Arrays.asList(100f, 200f, 300f)));
        assertFalse(MeasurementsFilter.needNormalization(Arrays.asList(350f, 320f)));
        assertTrue(MeasurementsFilter.needNormalization(Arrays.asList(1f, 350f, 3f)));
        assertTrue(MeasurementsFilter.needNormalization(Arrays.asList(80f, 300f, 40f)));
    }

    @Test
    public void testNormalization() {
        assertThat(MeasurementsFilter.normalize(Arrays.asList(1f, 2f, 3f)),
                is(Arrays.asList(181f, 182f, 183f)));

        assertThat(MeasurementsFilter.normalize(Arrays.asList(350f, 2f, 3f)),
                is(Arrays.asList(170f, 182f, 183f)));
    }

    @Test
    public void testRestoreNormalization() {
        assertEquals(5f, MeasurementsFilter.restoreInitial(185), 0f);
        assertEquals(350f, MeasurementsFilter.restoreInitial(170), 0f);
    }

}
