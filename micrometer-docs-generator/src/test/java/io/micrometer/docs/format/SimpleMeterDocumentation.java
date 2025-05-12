package io.micrometer.docs.format;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.docs.MeterDocumentation;

public enum SimpleMeterDocumentation implements MeterDocumentation {

    /**
     * A simple counter.
     */
    SIMPLE_COUNTER {
        @Override
        public String getName() {
            return "simple counter";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.COUNTER;
        }
    }
}
