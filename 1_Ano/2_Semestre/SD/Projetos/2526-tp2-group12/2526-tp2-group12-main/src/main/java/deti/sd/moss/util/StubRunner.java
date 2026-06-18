package deti.sd.moss.util;

import java.util.function.Function;
import java.util.function.Supplier;

import io.grpc.StatusRuntimeException;

public class StubRunner<Res> {
    private Res result;
    private StatusRuntimeException exception;

    private StubRunner(Supplier<Res> stubCall) {
        try {
            this.result = stubCall.get();
        } catch (StatusRuntimeException e) {
            this.exception = e;
        }
    }

    public static <Res> StubRunner<Res> execute(Supplier<Res> stubCall) {
        return new StubRunner<>(stubCall);
    }

    // Returns the result or a provided fallback object
    public Res orElse(Res fallback) {
        return exception == null ? result : fallback;
    }

    // Returns the result or executes a fallback lambda
    public Res orFail(Supplier<Res> fallbackAction) {
        return exception == null ? result : fallbackAction.get();
    }

    public Res orThrow() throws Exception {
        if (exception != null) throw new Exception("Service Unavailable", exception);
        return result;
    }
}
