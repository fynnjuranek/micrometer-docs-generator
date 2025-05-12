package io.micrometer.docs.format;

import io.micrometer.observation.docs.ObservationDocumentation;

public enum SimpleObservationDocumentation implements ObservationDocumentation {

    /**
     * A simple observation.
     */
    SIMPLE_OBSERVATION {
        @Override
        public String getName() {
            return "simple convention";
        }
    }
}
