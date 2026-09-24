// =============================================================
//  module-info.java — Configuração do Sistema de Módulos Java
//  Necessário para JavaFX funcionar corretamente com Java 17+
// =============================================================
module com.academia {

    // ── Módulos JavaFX necessários ────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // ── AtlantaFX (tema) ──────────────────────────────────────
    requires atlantafx.base;

    // ── SQLite JDBC ───────────────────────────────────────────
    requires org.xerial.sqlitejdbc;

    // OpenPDF
    requires com.github.librepdf.openpdf;

    // ── Java standard library ─────────────────────────────────
    requires java.sql;
    requires java.desktop;

    // ── Abre os pacotes para reflexão do JavaFX/FXML ─────────
    opens com.academia             to javafx.fxml;
    opens com.academia.controller  to javafx.fxml;
    opens com.academia.model       to javafx.base, javafx.fxml;

    // ── Exporta os pacotes principais ─────────────────────────
    exports com.academia;
    exports com.academia.controller;
    exports com.academia.model;
    exports com.academia.dao;
    exports com.academia.observer;
    exports com.academia.database;
    exports com.academia.util;
}

