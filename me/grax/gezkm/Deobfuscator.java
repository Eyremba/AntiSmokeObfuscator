package me.grax.gezkm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;

import me.grax.gezkm.utils.InstructionUtils;
import me.grax.gezkm.utils.MethodUtils;
import me.lpk.analysis.Sandbox;
import me.lpk.util.AccessHelper;
import me.lpk.util.OpUtils;

public class Deobfuscator implements Opcodes {

	public static void startDeobfuscation(Map<String, ClassNode> classes, Map<String, byte[]> other) {
		classes.values().forEach(cn -> {
			deobfuscateNumberObf(cn);
			cn.methods.forEach(mn -> {
				deobfuscateNumberFlow(mn);
				deobfuscateJumps(mn);
				removeDeadCode(cn.name, mn);
				remodeDeadTryCatch(mn);
				removeDeadCode(cn.name, mn);
				
				//need to run seperately
				deobfuscateStrings(classes, cn, mn);
			});
		});
	}

	private static void deobfuscateStrings(Map<String, ClassNode> classes, ClassNode cn, MethodNode mn) {
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getOpcode() == INVOKESTATIC) {
				MethodInsnNode min = (MethodInsnNode) ain;
				if (min.desc.equals("(Ljava/lang/String;I)Ljava/lang/String;") && min.owner.equals(cn.name)) {
					if (InstructionUtils.isNumber(ain.getPrevious()) && ain.getPrevious().getPrevious() instanceof LdcInsnNode) {
						int num = InstructionUtils.getIntValue(ain.getPrevious());
						String str = InstructionUtils.getStringValue(ain.getPrevious().getPrevious());
						MethodNode decrypt = MethodUtils.getMethod(cn, min.name, min.desc);
						MethodNode ref = MethodUtils.getFirstReference(classes, decrypt);
						if (ref == null) {
							//should not happen
							Object value = Sandbox.getIsolatedReturn(cn, new MethodInsnNode(INVOKESTATIC, cn.name, decrypt.name, decrypt.desc),
									new Object[] { str });
							if (value != null) {
								mn.instructions.set(min, new LdcInsnNode(value));
							}
						} else {
							Object value = Sandbox.getIsolatedReturn(classes.get(ref.owner), new MethodInsnNode(INVOKESTATIC, ref.owner, ref.name, ref.desc),
									new Object[] { str });
							if (value != null) {
								mn.instructions.remove(min.getPrevious().getPrevious());
								mn.instructions.remove(min.getPrevious());
								mn.instructions.set(min, new LdcInsnNode(value));
							}
						}
					}
				}
			}
		}
	}

	private static void deobfuscateNumberObf(ClassNode cn) {
		HashMap<String, Integer> returns = new HashMap<>();
		for (MethodNode mn : cn.methods) {
			if (mn.desc.equals("()I") && AccessHelper.isStatic(mn.access) && MethodUtils.hasNoRefs(mn)) {
				Object value = Sandbox.getIsolatedReturn(cn, new MethodInsnNode(INVOKESTATIC, cn.name, mn.name, mn.desc), new Class[0]);
				if (value != null) {
					returns.put(mn.name, (Integer) value);
				}
			}
		}
		for (MethodNode mn : cn.methods) {
			for (AbstractInsnNode ain : mn.instructions.toArray()) {
				if (ain.getOpcode() == INVOKESTATIC) {
					MethodInsnNode min = (MethodInsnNode) ain;
					if (min.desc.equals("()I")) {
						Integer ret = returns.get(min.name);
						if (ret != null) {
							mn.instructions.set(min, InstructionUtils.generateIntPush(ret));
						}
					}
				}
			}
		}
	}

	private static void deobfuscateNumberFlow(MethodNode mn) {
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getPrevious() != null && ain.getNext() != null)
				if (InstructionUtils.isNumber(ain) && InstructionUtils.isNumber(ain.getPrevious()) && ain.getNext().getOpcode() == SWAP) {
					int num1 = InstructionUtils.getIntValue(ain);
					int num2 = InstructionUtils.getIntValue(ain.getPrevious());
					if (num1 == num2) {
						if (ain.getNext().getNext() != null)
							if (ain.getNext().getNext().getOpcode() == POP) {
								mn.instructions.remove(ain.getPrevious());
								mn.instructions.remove(ain.getNext().getNext());
							}

						mn.instructions.remove(ain.getNext()); //remove useless swaps
					}
				}
			//			if (ain.getOpcode() == POP && ain.getPrevious().getOpcode() == DUP) {
			//				mn.instructions.remove(ain.getPrevious());
			//				mn.instructions.remove(ain);
			//			}
		}
	}

	private static void remodeDeadTryCatch(MethodNode mn) {
		Outer: for (TryCatchBlockNode tcbn : new ArrayList<>(mn.tryCatchBlocks)) {
			AbstractInsnNode handler = tcbn.handler;
			if (handler != null) {
				while ((handler instanceof LabelNode) || (handler instanceof LineNumberNode) || (handler instanceof FrameNode)) {
					handler = handler.getNext();
					if (handler == null) {
						continue Outer;
					}
				}
				if (handler.getOpcode() == ATHROW) {
					mn.tryCatchBlocks.remove(tcbn); //useless trycatch
				}
			}
		}
	}

	private static void deobfuscateJumps(MethodNode mn) {
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getOpcode() >= IFEQ && ain.getOpcode() <= IFLE) {
				JumpInsnNode jump = (JumpInsnNode) ain;
				if (InstructionUtils.isNumber(ain.getPrevious())) { //shouldn't be null because its impossible
					int num = InstructionUtils.getIntValue(ain.getPrevious());
					if (InstructionUtils.isGoto(jump, num)) {
						mn.instructions.remove(ain.getPrevious());
						mn.instructions.set(jump, new JumpInsnNode(GOTO, jump.label));
					} else {
						mn.instructions.remove(ain.getPrevious());
						mn.instructions.remove(jump);
					}
				}
			}
		}
	}

	private static void removeDeadCode(String clazz, MethodNode method) {
		try {
			Analyzer analyzer = new Analyzer(new BasicInterpreter());
			analyzer.analyze(clazz, method);
			Frame[] frames = analyzer.getFrames();
			AbstractInsnNode[] insns = method.instructions.toArray();
			for (int i = 0; i < frames.length; i++) {
				AbstractInsnNode insn = insns[i];
				if (frames[i] == null && insn.getType() != AbstractInsnNode.LABEL) {
					method.instructions.remove(insn);
					insns[i] = null;
				}
			}
		} catch (AnalyzerException e) {
		}
	}
}
