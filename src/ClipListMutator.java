import java.util.List;

/** for lambdas that mutate a List<String> in place */
@FunctionalInterface
interface ListTransformer {
    void transform(List<String> list);
}

/** for lambdas that take a line and return a new line */
@FunctionalInterface
interface LineTransformer {
    String transform(String line);
}

abstract class ClipboardAsListMutatorBase {
    public List<String> mutate() {
        List<String> result = SysClipboard.readAsLines();
        mutateImpl(result);
        SysClipboard.write(String.join("\n", result));
        return result;
    }
    protected abstract void mutateImpl(List<String> result);
}

class ClipboardAsListMutator extends ClipboardAsListMutatorBase {
    private ListTransformer listMutator;
    ClipboardAsListMutator(ListTransformer mutatorFunction) {
        listMutator = mutatorFunction;
    }
    protected void mutateImpl(List<String> result) {
        listMutator.transform(result);
    }
}

class ClipboardAsListMutatorByLine extends ClipboardAsListMutatorBase {
    private LineTransformer lineMutator;
    ClipboardAsListMutatorByLine(LineTransformer mutatorFunction) {
        lineMutator = mutatorFunction;
    }
    protected void mutateImpl(List<String> result) {
        for (int i = 0; i < result.size(); ++i) {
            result.set(i, lineMutator.transform(result.get(i)));
        }
    }
}
