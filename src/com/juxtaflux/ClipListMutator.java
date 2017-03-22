package com.juxtaflux;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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

/** Base class (contains shared code) for mutators that treat clipboard as a list */
abstract class ClipboardAsListMutatorBase {
    public List<String> mutate() {
        List<String> result = SysClipboard.readAsLines();
        mutateImpl(result);
        SysClipboard.write(String.join(System.lineSeparator(), result));
        return result;
    }
    protected abstract void mutateImpl(List<String> result);
}

/** Treat the clipboard contents as a list and mutate the list by the given ListTransformer */
class ClipboardAsListMutator extends ClipboardAsListMutatorBase {
    private ListTransformer listMutator;
    ClipboardAsListMutator(ListTransformer mutatorFunction) {
        listMutator = mutatorFunction;
    }
    protected void mutateImpl(List<String> result) {
        listMutator.transform(result);
    }
}

/** Treat the clipboard contents as a list and mutate each line by given LineTransformer */
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

/** Treat the clipboard contents as a list and filter it by the given predicate */
class ClipboardAsListFilter {
    private Predicate<String> filterPredicate;
    ClipboardAsListFilter(Predicate<String> predicate) {
        filterPredicate = predicate;
    }
    public Pair<List<String>, List<String>> filter() {
        List<String> list = SysClipboard.readAsLines();
        List<String> filtered = new ArrayList();
        for (String line : list) {
            if (filterPredicate.test(line)) {
                filtered.add(line);
            }
        }
        SysClipboard.write(String.join(System.lineSeparator(), filtered));
        return Pair.of(list, filtered);
    }
}