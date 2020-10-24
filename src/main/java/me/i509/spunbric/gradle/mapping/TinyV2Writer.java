package me.i509.spunbric.gradle.mapping;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.TextMappingsWriter;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.gradle.api.logging.Logger;

final class TinyV2Writer extends TextMappingsWriter {
	private final Logger logger;
	private final String obfNamespace;
	private final String deobfNamespace;

	TinyV2Writer(Logger logger, String obfNamespace, String deobfNamespace, Writer writer) {
		super(writer);
		this.logger = logger;
		this.obfNamespace = obfNamespace;
		this.deobfNamespace = deobfNamespace;
	}

	@Override
	public void write(MappingSet mappings) throws IOException {
		//this.validateMappings(mappings);

		// Write the header, below for example
		// tiny	2	0	official	intermediary
		this.writer.print("tiny");
		printTab();
		this.writer.print("2");
		printTab();
		this.writer.print("0");
		printTab();
		this.writer.print(this.obfNamespace);
		printTab();
		this.writer.print(this.deobfNamespace);

		MappingUtils.iterateClasses(mappings, classMapping -> {
			this.writer.println(); // Always NL before next class
			this.writer.print("c"); // Class
			this.printTab();
			this.writer.print(classMapping.getFullObfuscatedName()); // Obf
			this.printTab();
			this.writer.print(classMapping.getFullDeobfuscatedName()); // Deobf

			// Methods
			for (MethodMapping methodMapping : classMapping.getMethodMappings()) {
				this.writer.println(); // Newline for method
				this.printTab(); // Indent by 1
				this.writer.print("m"); // Method
				this.printTab();
				this.writer.print(methodMapping.getSignature().getDescriptor().toString()); // Method descriptor
				this.printTab();
				this.writer.print(methodMapping.getSimpleObfuscatedName()); // Obf
				this.printTab();
				this.writer.print(methodMapping.getSimpleDeobfuscatedName()); // Deobf
			}

			// Fields
			for (FieldMapping fieldMapping : classMapping.getFieldMappings()) {
				// Only write if we have a field mapping
				fieldMapping.getType().ifPresent(type -> {
					this.writer.println(); // Newline for field
					this.printTab();
					this.writer.print("f");
					this.printTab();

					// Write the field type. This is required for tiny spec
					this.writer.print(type);

					this.printTab();
					this.writer.print(fieldMapping.getObfuscatedName());
					this.printTab();
					this.writer.print(fieldMapping.getDeobfuscatedName());
				});
			}
		});
	}

	private void validateMappings(MappingSet mappings) {
		final List<MappingUtils.MissingFieldSignatureEntry> fieldsWithMissingSignature = MappingUtils.findFieldsWithMissingSignature(mappings);

		if (!fieldsWithMissingSignature.isEmpty()) {
			this.logger.error(fieldsWithMissingSignature.size() + " field descriptors were missing in official -> srg mappings:");

			for (MappingUtils.MissingFieldSignatureEntry missingFieldSignatureEntry : fieldsWithMissingSignature) {
				this.logger.error(missingFieldSignatureEntry.toString());
			}

			throw new RuntimeException(fieldsWithMissingSignature.size() + " field descriptors were missing in official -> srg mappings");
		}
	}

	private void printTab() {
		this.writer.print("\t");
	}
}
