package io.micrometer.docs.format;

import io.micrometer.tracing.docs.SpanDocumentation;

public enum SimpleSpanDocumentation implements SpanDocumentation {

    /**
     * A simple span.
     */
    SIMPLE_SPAN {
        @Override
        public String getName() {
            return "simple span";
        }
    }
}
