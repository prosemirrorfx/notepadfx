package io.github.prosemirrorfx.notepadfx.doc;

import datapotter.datahelper.Data;
import java.nio.file.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Data
public final class FileText extends FileText_A {
    static final String BOM = "﻿";
    String text;
    Boolean crlf;
    Boolean bom;

    static FileText read(Path p) {
        try {
            var raw = Files.exists(p) ? Files.readString(p, UTF_8) : "";
            var bom = raw.startsWith(BOM);
            if (bom) raw = raw.substring(1);
            return new FileText().text(raw.replace("\r\n", "\n")).crlf(raw.contains("\r\n")).bom(bom);
        } catch (Exception e) {
            return new FileText().text("Could not read " + p + "\n" + e).crlf(false).bom(false);
        }
    }

    byte[] encode(String t) {
        var s = crlf ? t.replace("\n", "\r\n") : t;
        return ((bom ? BOM : "") + s).getBytes(UTF_8);
    }
}
