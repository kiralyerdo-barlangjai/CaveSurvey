package com.astoev.cave.survey.test.util;

import com.astoev.cave.survey.service.orientation.MeasurementsFilter;

import junit.framework.TestCase;

import org.junit.Test;

public class MeasurementsFilterTest extends TestCase {

    @Test
    public void testAveragingDisabled() {
        // filter without averaging
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(false);
        assertFalse(filter.isReady());

        // value returned as is
        float value1 = 1.23F;
        filter.addMeasurement(value1);
        assertTrue(filter.isReady());
        assertEquals(value1, filter.getValue());
        assertEquals( "(0.0 out of 1)", filter.getAccuracyString());

        // last value always used
        float value2 = 2.34F;
        filter.addMeasurement(value2);
        assertTrue(filter.isReady());
        assertEquals(value2, filter.getValue());
        assertEquals( "(0.0 out of 1)", filter.getAccuracyString());

        // averaging 1 out of 1
        filter.startAveraging();
        filter.addMeasurement(value1);
        assertTrue(filter.isReady());
        assertEquals(value1, filter.getValue());
        assertEquals( "(0.0 out of 1)", filter.getAccuracyString());
    }

    @Test
    public void testAveragingEnabled() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(2);
        filter.startAveraging();

        filter.addMeasurement(1f);
        filter.addMeasurement(2f);
        assertTrue(filter.isReady());
        assertEquals(1.5, filter.getValue(), 0.0);
        assertEquals( "(0.5 out of 2)", filter.getAccuracyString());
    }

    @Test
    public void testPlusMinus() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(2);
        filter.startAveraging();

        filter.addMeasurement(-1f);
        filter.addMeasurement(1f);
        assertEquals(0, filter.getValue(), 0);
        assertEquals( "(1.0 out of 2)", filter.getAccuracyString());
    }

    @Test
    public void test360Degrees() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(2);
        filter.startAveraging();

        filter.addMeasurement(355f);
        filter.addMeasurement(15f);
        assertEquals(5, filter.getValue(), 0.0);

        filter.addMeasurement(45f);
        filter.addMeasurement(47f);
        assertEquals(46, filter.getValue(), 0.0);

        filter.addMeasurement(89f);
        filter.addMeasurement(91f);
        assertEquals(90, filter.getValue(), 0.0);

        filter.addMeasurement(145f);
        filter.addMeasurement(147f);
        assertEquals(146, filter.getValue(), 0.0);

        filter.addMeasurement(229f);
        filter.addMeasurement(231f);
        assertEquals(230, filter.getValue(), 0.0);

        filter.addMeasurement(355f);
        filter.addMeasurement(5f);
        assertEquals(0, filter.getValue(), 0.0);
    }

    @Test
    public void testMultiple() {
        MeasurementsFilter filter = new MeasurementsFilter();
        filter.setAveragingEnabled(true);
        filter.setNumMeasurements(5);
        filter.startAveraging();

        filter.addMeasurement(1f);
        filter.addMeasurement(2f);
        filter.addMeasurement(3f);
        filter.addMeasurement(4f);
        filter.addMeasurement(5f);
        assertEquals(3, filter.getValue(), 0.0);
        assertEquals( "(2.0 out of 5)", filter.getAccuracyString());
    }

}
