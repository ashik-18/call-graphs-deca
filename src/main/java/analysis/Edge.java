package analysis;

import javax.annotation.Nonnull;

public class Edge<S, T> {

    @Nonnull
    public final S source;
    @Nonnull
    public final T target;

    public Edge(@Nonnull S source, @Nonnull T target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge<?, ?> edge = (Edge<?, ?>) o;

        if (!source.equals(edge.source)) return false;
        return target.equals(edge.target);
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }

    @Override
    @Nonnull
    public String toString() {
        return "(" +
                source +
                " --> " + target +
                ')';
    }
}
