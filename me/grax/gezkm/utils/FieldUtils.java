package me.grax.gezkm.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import me.lpk.util.AccessHelper;

public class FieldUtils {

	public static ArrayList<FieldNode> getPossible(ClassNode cn) {
		ArrayList<FieldNode> nodes = new ArrayList<>();
		cn.fields.forEach(field -> {
			if (field.desc.equals("[Ljava/lang/String;") && AccessHelper.isStatic(field.access)) {
				nodes.add(field);
			}
		});
		return nodes;
	}

	public static FieldNode findTreeField(ClassNode cn, Field f) {
		for (FieldNode fn : cn.fields) {
			if (fn.name.equals(f.getName()) && fn.desc.equals(DescUtils.getDescriptorForClass(f.getType()))) {
				return fn;
			}
		}
		return null;
	}
}
