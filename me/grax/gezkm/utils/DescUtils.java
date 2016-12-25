package me.grax.gezkm.utils;

public class DescUtils {
	public static String getDescriptorForClass(final Class c) {
		if (c.isPrimitive()) {
			if (c == byte.class)
				return "B";
			if (c == char.class)
				return "C";
			if (c == double.class)
				return "D";
			if (c == float.class)
				return "F";
			if (c == int.class)
				return "I";
			if (c == long.class)
				return "J";
			if (c == short.class)
				return "S";
			if (c == boolean.class)
				return "Z";
			if (c == void.class)
				return "V";
			if (c.isArray())
				return c.getName().replace('.', '/');
			throw new RuntimeException("Unrecognized primitive " + c + " " + c.getName());
		}
		if (c.isArray())
			return c.getName().replace('.', '/');
		return ('L' + c.getName() + ';').replace('.', '/');
	}
}
