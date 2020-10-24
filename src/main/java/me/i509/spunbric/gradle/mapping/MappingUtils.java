package me.i509.spunbric.gradle.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.cadixdev.bombe.type.ArrayType;
import org.cadixdev.bombe.type.FieldType;
import org.cadixdev.bombe.type.ObjectType;
import org.cadixdev.bombe.type.PrimitiveType;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;

final class MappingUtils {
	static void iterateClasses(MappingSet mappings, Consumer<ClassMapping<?, ?>> action) {
		for (TopLevelClassMapping classMapping : mappings.getTopLevelClassMappings()) {
			iterateClass(classMapping, action);
		}
	}

	private static void iterateClass(ClassMapping<?, ?> classMapping, Consumer<ClassMapping<?, ?>> action) {
		action.accept(classMapping);

		for (InnerClassMapping innerClassMapping : classMapping.getInnerClassMappings()) {
			action.accept(innerClassMapping);
		}
	}

	static List<MissingFieldSignatureEntry> findFieldsWithMissingSignature(MappingSet mappings) {
		final List<MissingFieldSignatureEntry> list = new ArrayList<>();

		iterateClasses(mappings, classMapping -> {
			for (FieldMapping fieldMapping : classMapping.getFieldMappings()) {
				if (!fieldMapping.getType().isPresent()) {
					list.add(new MissingFieldSignatureEntry(classMapping.getObfuscatedName(), classMapping.getDeobfuscatedName(), fieldMapping.getObfuscatedName(), fieldMapping.getDeobfuscatedName()));
				}
			}
		});

		return list;
	}

	static Optional<FieldType> remapFieldType(MappingSet mappings, FieldType fieldType) {
		Objects.requireNonNull(fieldType, "Cannot remap null field type");

		// No need to remap primitives or void types
		if (fieldType instanceof PrimitiveType) {
			return Optional.of(fieldType);
		}

		// Handle arrays
		if (fieldType instanceof ArrayType) {
			return remapFieldType(mappings, ((ArrayType) fieldType).getComponent())
					.map(componentType -> new ArrayType(((ArrayType) fieldType).getDimCount(), componentType));
		}

		if (fieldType instanceof ObjectType) {
			return mappings.getClassMapping(((ObjectType) fieldType).getClassName())
					.map(classMapping -> new ObjectType(classMapping.getDeobfuscatedName()));
		}

		throw new RuntimeException("Unsupported field type: " + fieldType);
	}

	private MappingUtils() {
	}

	static class MissingFieldSignatureEntry {
		private final String obfuscatedClassName;
		private final String deobfuscatedClassName;
		private final String obfuscatedFieldName;
		private final String deobfuscatedFieldName;

		MissingFieldSignatureEntry(String obfuscatedClassName, String deobfuscatedClassName, String obfuscatedFieldName, String deobfuscatedFieldName) {
			this.obfuscatedClassName = obfuscatedClassName;
			this.deobfuscatedClassName = deobfuscatedClassName;
			this.obfuscatedFieldName = obfuscatedFieldName;
			this.deobfuscatedFieldName = deobfuscatedFieldName;
		}

		@Override
		public String toString() {
			return "[" + this.obfuscatedClassName + " -> " +  this.deobfuscatedClassName + "] is missing a field descriptor for [" + this.obfuscatedFieldName + " -> " + this.deobfuscatedFieldName + "]";
		}
	}
}
