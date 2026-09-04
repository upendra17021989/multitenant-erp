package com.multitenanterp.attendance;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceCsvParserTest {
    @Test void parsesQuotedCommasEscapedQuotesAndWindowsLines(){
        var rows=AttendanceCsvImportService.parse("employeeNumber,attendanceDate,notes\r\nEMP01,2026-09-04,\"Late, approved\"\r\nEMP02,2026-09-04,\"Said \"\"hello\"\"\"");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(1)).containsExactly("EMP01","2026-09-04","Late, approved");
        assertThat(rows.get(2).get(2)).isEqualTo("Said \"hello\"");
    }

    @Test void rejectsUnterminatedQuotes(){
        assertThatThrownBy(()->AttendanceCsvImportService.parse("employeeNumber,notes\nEMP01,\"broken"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
