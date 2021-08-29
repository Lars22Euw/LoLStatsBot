package util.create;

import util.U;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class AnyTypeRef<T> implements ParameterizedType {

    AnyTypeRef<?> owner;
    Class<T> clazz;
    List<AnyTypeRef<?>> subtypes;

    public static <T> AnyTypeRef<T> of(Class<T> main, Class<?>... classes) {
        return new AnyTypeRef(null, impl(main),
                Arrays.stream(classes)
                        .map(clz -> new AnyTypeRef(null, clz, new ArrayList<>()))
                        .collect(Collectors.toList()));
    }

    private static <T> Class impl(Class<T> main) {
        if (main == List.class) {
            return ArrayList.class;
        } else if (main == Map.class) {
            return HashMap.class;
        } else if (main == Set.class) {
            return HashSet.class;
        }
        return main;
    }

    public static <T> AnyTypeRef<T> of(Class<T> clazz, List<AnyTypeRef> subtypes) {
        var result = new AnyTypeRef(null, impl(clazz), subtypes);
        subtypes.forEach(tr -> tr.owner = result);
        return result;
    }

    public static AnyTypeRef of(AnyTypeRef... types) {
        if (types.length < 1) {
            return null;
        }
        var subTypes = Arrays.stream(types).skip(1).collect(Collectors.toList());
        subTypes.forEach(tr -> tr.owner = types[0]);
        types[0].subtypes = subTypes;
        return types[0];
    }

    private AnyTypeRef(AnyTypeRef<?> owner, Class<T> clazz, List<AnyTypeRef<?>> subtypes) {
        this.owner = owner;
        this.clazz = clazz;
        this.subtypes = subtypes;
    }

    public static <T> AnyTypeRef<?> of(AnyTypeRef<T> of, AnyTypeRef<?>... toArray) {
        of.subtypes = Arrays.stream(toArray).collect(Collectors.toList());
        of.subtypes.forEach(tr -> tr.owner = of);
        return of;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[0];
    }

    @Override
    public Type getRawType() {
        return null;
    }

    @Override
    public Type getOwnerType() {
        return owner;
    }

    @Override
    public String getTypeName() {
        return clazz.getSimpleName() + "<" + U.join(subtypes, ",", AnyTypeRef::getTypeName) + ">";
    }

    public boolean typeEquals(Object o) {
        return typeEquals(this, o);
    }

    public static boolean typeEquals(AnyTypeRef<?> thisOne, Object o) {
        if (thisOne == o) return true;
        if (o == null) return false;
        if (o instanceof Type) {
            return thisOne.subtypes.isEmpty() && thisOne.clazz.equals(o);
        }
        if (o instanceof ParameterizedType) {
            var that = (ParameterizedType) o;
            if (((Class<?>) that.getRawType()).isAssignableFrom(thisOne.clazz)) {
                return false;
            }
            return U.all(U.zipMap(thisOne.subtypes, Arrays.asList(that.getActualTypeArguments()),
                    (subTr, type) -> typeEquals(subTr, type)));
        }
        return false;
    }
}
