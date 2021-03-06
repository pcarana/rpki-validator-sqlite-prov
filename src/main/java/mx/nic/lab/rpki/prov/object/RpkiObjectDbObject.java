package mx.nic.lab.rpki.prov.object;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.nic.lab.rpki.db.exception.ValidationError;
import mx.nic.lab.rpki.db.exception.ValidationException;
import mx.nic.lab.rpki.db.pojo.RpkiObject;

public class RpkiObjectDbObject extends RpkiObject implements DatabaseObject {

	public static final String ID_COLUMN = "rpo_id";
	public static final String TYPE_COLUMN = "rpo_type";
	public static final String SERIAL_NUMBER_COLUMN = "rpo_serial_number";
	public static final String SIGNING_TIME_COLUMN = "rpo_signing_time";
	public static final String LAST_MARK_REACHABLE_AT_COLUMN = "rpo_last_marked_reachable_at";
	public static final String AUTHORITY_KEY_IDENTIFIER_COLUMN = "rpo_authority_key_identifier";
	public static final String SUBJECT_KEY_IDENTIFIER_COLUMN = "rpo_subject_key_identifier";
	public static final String SHA256_COLUMN = "rpo_sha256";
	public static final String IS_CA_COLUMN = "rpo_is_ca";

	/**
	 * Mapping of the {@link RpkiObject} properties to its corresponding DB column
	 */
	public static final Map<String, String> propertyToColumnMap;
	static {
		propertyToColumnMap = new HashMap<>();
		propertyToColumnMap.put(ID, ID_COLUMN);
		propertyToColumnMap.put(TYPE, TYPE_COLUMN);
		propertyToColumnMap.put(SERIAL_NUMBER, SERIAL_NUMBER_COLUMN);
		propertyToColumnMap.put(SIGNING_TIME, SIGNING_TIME_COLUMN);
		propertyToColumnMap.put(LAST_MARK_REACHABLE_AT, LAST_MARK_REACHABLE_AT_COLUMN);
		propertyToColumnMap.put(AUTHORITY_KEY_IDENTIFIER, AUTHORITY_KEY_IDENTIFIER_COLUMN);
		propertyToColumnMap.put(SUBJECT_KEY_IDENTIFIER, SUBJECT_KEY_IDENTIFIER_COLUMN);
		propertyToColumnMap.put(SHA256, SHA256_COLUMN);
		propertyToColumnMap.put(IS_CA, IS_CA_COLUMN);
	}

	public RpkiObjectDbObject() {
		super();
	}

	public RpkiObjectDbObject(RpkiObject rpkiObject) {
		this.setId(rpkiObject.getId());
		this.setType(rpkiObject.getType());
		this.setSerialNumber(rpkiObject.getSerialNumber());
		this.setSigningTime(rpkiObject.getSigningTime());
		this.setLastMarkedReachableAt(rpkiObject.getLastMarkedReachableAt());
		this.setAuthorityKeyIdentifier(rpkiObject.getAuthorityKeyIdentifier());
		this.setSubjectKeyIdentifier(rpkiObject.getSubjectKeyIdentifier());
		this.setSha256(rpkiObject.getSha256());
		this.setIsCa(rpkiObject.isCa());
		this.setEncodedRpkiObject(rpkiObject.getEncodedRpkiObject());
		this.setRpkiRepositories(rpkiObject.getRpkiRepositories());
		this.setLocations(rpkiObject.getLocations());
		this.setRoas(rpkiObject.getRoas());
		this.setGbr(rpkiObject.getGbr());
	}

	public RpkiObjectDbObject(ResultSet resultSet) throws SQLException {
		super();
		loadFromDatabase(resultSet);
	}

	@Override
	public void loadFromDatabase(ResultSet resultSet) throws SQLException {
		byte[] tempBytes = null;
		setId(resultSet.getLong(ID_COLUMN));
		if (resultSet.wasNull()) {
			setId(null);
		}
		setType(DatabaseObject.getStringAsEnum(Type.class, resultSet.getString(TYPE_COLUMN)));
		tempBytes = resultSet.getBytes(SERIAL_NUMBER_COLUMN);
		if (resultSet.wasNull()) {
			setSerialNumber(null);
		} else {
			setSerialNumber(new BigInteger(tempBytes));
		}
		setSigningTime(DatabaseObject.getStringDateAsInstant(resultSet.getString(SIGNING_TIME_COLUMN)));
		setLastMarkedReachableAt(
				DatabaseObject.getStringDateAsInstant(resultSet.getString(LAST_MARK_REACHABLE_AT_COLUMN)));
		setAuthorityKeyIdentifier(resultSet.getBytes(AUTHORITY_KEY_IDENTIFIER_COLUMN));
		if (resultSet.wasNull()) {
			setAuthorityKeyIdentifier(null);
		}
		setSubjectKeyIdentifier(resultSet.getBytes(SUBJECT_KEY_IDENTIFIER_COLUMN));
		if (resultSet.wasNull()) {
			setSubjectKeyIdentifier(null);
		}
		setSha256(resultSet.getBytes(SHA256_COLUMN));
		if (resultSet.wasNull()) {
			setSha256(null);
		}
		setIsCa(resultSet.getBoolean(IS_CA_COLUMN));
	}

	@Override
	public void storeToDatabase(PreparedStatement statement) throws SQLException {
		if (getType() != null) {
			statement.setString(1, getType().toString());
		} else {
			statement.setNull(1, Types.VARCHAR);
		}
		if (getSerialNumber() != null) {
			statement.setBytes(2, getSerialNumber().toByteArray());
		} else {
			statement.setNull(2, Types.BLOB);
		}
		if (getSigningTime() != null) {
			statement.setString(3, getSigningTime().toString());
		} else {
			statement.setNull(3, Types.VARCHAR);
		}
		if (getLastMarkedReachableAt() != null) {
			statement.setString(4, getLastMarkedReachableAt().toString());
		} else {
			statement.setNull(4, Types.VARCHAR);
		}
		if (getAuthorityKeyIdentifier() != null) {
			statement.setBytes(5, getAuthorityKeyIdentifier());
		} else {
			statement.setNull(5, Types.BLOB);
		}
		if (getSubjectKeyIdentifier() != null) {
			statement.setBytes(6, getSubjectKeyIdentifier());
		} else {
			statement.setNull(6, Types.BLOB);
		}
		if (getSha256() != null) {
			statement.setBytes(7, getSha256());
		} else {
			statement.setNull(7, Types.BLOB);
		}
		statement.setBoolean(8, isCa());
	}

	@Override
	public void validate(Operation operation) throws ValidationException {
		List<ValidationError> validationErrors = new ArrayList<>();
		if (operation == Operation.CREATE) {

		}
		if (!validationErrors.isEmpty()) {
			throw new ValidationException(validationErrors);
		}
	}
}
