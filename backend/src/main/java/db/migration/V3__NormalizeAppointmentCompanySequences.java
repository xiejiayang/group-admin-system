package db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__NormalizeAppointmentCompanySequences extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, Long> companySequences = new LinkedHashMap<>();
        try (Statement query = context.getConnection().createStatement();
                ResultSet records = query.executeQuery("""
                        SELECT id, company_name
                        FROM appointment_record
                        WHERE deleted = FALSE
                        ORDER BY display_sequence ASC, id ASC
                        """);
                PreparedStatement update = context.getConnection().prepareStatement("""
                        UPDATE appointment_record
                        SET global_sequence = ?
                        WHERE id = ?
                        """)) {
            while (records.next()) {
                String companyName = records.getString("company_name");
                // 总序号按“所属公司”的类别数量生成，同一公司名称下的记录共享同一个类别序号。
                long companySequence = companySequences.computeIfAbsent(
                        companyName,
                        ignored -> (long) companySequences.size() + 1);
                update.setLong(1, companySequence);
                update.setLong(2, records.getLong("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}
