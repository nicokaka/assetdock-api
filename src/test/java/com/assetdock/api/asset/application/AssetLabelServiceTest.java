package com.assetdock.api.asset.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetLabelServiceTest {

	private final AssetLabelService assetLabelService = new AssetLabelService();

	@Test
	void generatesPdfSafelyWithNewlinesAndNonWinAnsiCharacters() {
		byte[] pdf = assetLabelService.generateLabelPdf(
			"AST-001\nLINE2",
			"Laptop 🚀 日本語\tModel",
			"SN-中文-123"
		);

		assertThat(pdf).isNotEmpty();
		assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
	}
}
