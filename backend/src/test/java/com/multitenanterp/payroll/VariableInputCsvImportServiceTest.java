package com.multitenanterp.payroll;
import org.junit.jupiter.api.Test;import org.springframework.web.server.ResponseStatusException;import static org.assertj.core.api.Assertions.*;
class VariableInputCsvImportServiceTest{
 @Test void parsesQuotedNamesNotesAndWindowsLines(){var rows=VariableInputCsvImportService.parse("employeeNumber,code,name,inputType,amount,notes\r\nEMP01,BONUS,Bonus,EARNING,2500,\"Quarterly, approved\"\r\nEMP02,RECOVERY,Recovery,DEDUCTION,100,\"Said \"\"ok\"\"\"");assertThat(rows).hasSize(3);assertThat(rows.get(1)).containsExactly("EMP01","BONUS","Bonus","EARNING","2500","Quarterly, approved");assertThat(rows.get(2).get(5)).isEqualTo("Said \"ok\"");}
 @Test void rejectsUnterminatedQuotedValue(){assertThatThrownBy(()->VariableInputCsvImportService.parse("employeeNumber,notes\nEMP01,\"broken")).isInstanceOf(ResponseStatusException.class);}
}
