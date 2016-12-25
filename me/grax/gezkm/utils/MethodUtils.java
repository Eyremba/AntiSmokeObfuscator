package me.grax.gezkm.utils;

import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodUtils implements Opcodes {
	public static MethodNode getClinit(ClassNode cn) {
		return getMethod(cn, "<clinit>");
	}

	private static boolean containsFieldName(ClassNode cn, String string) {
		for (FieldNode fn : cn.fields) {
			if (fn.name.equals(string)) {
				return true;
			}
		}
		return false;
	}

	public static AbstractInsnNode generateIntPush(int i) {
		if (i <= 5 && i >= -1) {
			return new InsnNode(i + 3); //iconst_i
		}
		if (i >= -128 && i <= 127) {
			return new IntInsnNode(BIPUSH, i);
		}

		if (i >= -32768 && i <= 32767) {
			return new IntInsnNode(SIPUSH, i);
		}
		return new LdcInsnNode(i);
	}

	public static int getIntValue(AbstractInsnNode node) {
		if (node.getOpcode() >= ICONST_M1 && node.getOpcode() <= ICONST_5) {
			return node.getOpcode() - 3;
		}
		if (node.getOpcode() == SIPUSH || node.getOpcode() == BIPUSH) {
			return ((IntInsnNode) node).operand;
		}
		throw new RuntimeException("node does not contains an integer!");
	}

	public static MethodNode getMethod(ClassNode cn, String name) {
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals(name)) {
				return mn;
			}
		}
		return null;
	}

	public static MethodNode getMethod(ClassNode cn, String name, String desc) {
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals(name) && mn.desc.equals(desc)) {
				return mn;
			}
		}
		return null;
	}

	public static boolean hasNoRefs(MethodNode mn) {
		boolean noRefs = true;
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getOpcode() == INVOKEDYNAMIC || ain.getOpcode() == INVOKEINTERFACE || ain.getOpcode() == INVOKESPECIAL
					|| ain.getOpcode() == INVOKESTATIC || ain.getOpcode() == INVOKEVIRTUAL) {
				noRefs = false;
				break;
			}
			if (ain.getOpcode() == GETFIELD || ain.getOpcode() == GETSTATIC) {
				noRefs = false;
				break;
			}
		}
		return noRefs;
	}

	public static MethodNode getFirstReference(Map<String, ClassNode> nodes, MethodNode mn) {
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain instanceof MethodInsnNode) {
				MethodInsnNode min = (MethodInsnNode) ain;
				return getMethod(nodes.get(min.owner), min.name, min.desc);
			}
		}
		return null;
	}
}
