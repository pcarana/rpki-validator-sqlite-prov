package mx.nic.lab.rpki.sqlite.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import mx.nic.lab.rpki.db.exception.ValidationError;
import mx.nic.lab.rpki.db.exception.ValidationErrorType;
import mx.nic.lab.rpki.db.exception.ValidationException;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;

/**
 * Extension of {@link SlurmBgpsec} as a {@link DatabaseObject}
 *
 */
public class SlurmBgpsecDbObject extends SlurmBgpsec implements DatabaseObject {

	public static final String ID_COLUMN = "slb_id";
	public static final String ASN_COLUMN = "slb_asn";
	public static final String SKI_COLUMN = "slb_ski";
	public static final String PUBLIC_KEY_COLUMN = "slb_public_key";
	public static final String TYPE_COLUMN = "slb_type";
	public static final String COMMENT_COLUMN = "slb_comment";

	public SlurmBgpsecDbObject() {
		super();
	}

	/**
	 * Build an instance from a {@link SlurmBgpsec}
	 * 
	 * @param slurmBgpsec
	 */
	public SlurmBgpsecDbObject(SlurmBgpsec slurmBgpsec) {
		this.setId(slurmBgpsec.getId());
		this.setAsn(slurmBgpsec.getAsn());
		this.setSki(slurmBgpsec.getSki());
		this.setPublicKey(slurmBgpsec.getPublicKey());
		this.setType(slurmBgpsec.getType());
		this.setComment(slurmBgpsec.getComment());
	}

	/**
	 * Create a new instance loading values from a <code>ResultSet</code>
	 * 
	 * @param resultSet
	 * @throws SQLException
	 */
	public SlurmBgpsecDbObject(ResultSet resultSet) throws SQLException {
		super();
		loadFromDatabase(resultSet);
	}

	@Override
	public void loadFromDatabase(ResultSet resultSet) throws SQLException {
		setId(resultSet.getLong(ID_COLUMN));
		if (resultSet.wasNull()) {
			setId(null);
		}
		setAsn(resultSet.getLong(ASN_COLUMN));
		if (resultSet.wasNull()) {
			setAsn(null);
		}
		setSki(resultSet.getString(SKI_COLUMN));
		setPublicKey(resultSet.getString(PUBLIC_KEY_COLUMN));
		setType(resultSet.getInt(TYPE_COLUMN));
		if (resultSet.wasNull()) {
			setType(null);
		}
		setComment(resultSet.getString(COMMENT_COLUMN));
	}

	@Override
	public void storeToDatabase(PreparedStatement statement) throws SQLException {
		statement.setLong(1, getId());
		if (getAsn() != null) {
			statement.setLong(2, getAsn());
		} else {
			statement.setNull(2, Types.NUMERIC);
		}
		if (getSki() != null) {
			statement.setString(3, getSki());
		} else {
			statement.setNull(3, Types.VARCHAR);
		}
		if (getPublicKey() != null) {
			statement.setString(4, getPublicKey());
		} else {
			statement.setNull(4, Types.VARCHAR);
		}
		if (getType() != null) {
			statement.setInt(5, getType());
		} else {
			statement.setNull(5, Types.INTEGER);
		}
		if (getComment() != null) {
			statement.setString(6, getComment());
		} else {
			statement.setNull(6, Types.VARCHAR);
		}
	}

	@Override
	public void validate(Operation operation) throws ValidationException {
		List<ValidationError> validationErrors = new ArrayList<>();
		if (operation == Operation.CREATE) {
			// Check the attributes according to the type
			// The ID isn't validated since this is a new object
			Integer type = this.getType();
			Long asn = this.getAsn();
			String ski = getTrimmedString(this.getSki());
			String publicKey = getTrimmedString(this.getPublicKey());
			String comment = getTrimmedString(this.getComment());

			if (type == null) {
				validationErrors.add(new ValidationError(OBJECT_NAME, TYPE, null, ValidationErrorType.NULL));
			} else if (type == TYPE_FILTER) {
				// Either an ASN or a SKI must exist
				if (asn == null && ski == null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, ASN, null, ValidationErrorType.NULL));
					validationErrors.add(new ValidationError(OBJECT_NAME, SKI, null, ValidationErrorType.NULL));
				}
				// Public key is only for assertions
				if (publicKey != null) {
					validationErrors
							.add(new ValidationError(OBJECT_NAME, PUBLIC_KEY, publicKey, ValidationErrorType.NOT_NULL));
				}
			} else if (type == TYPE_ASSERTION) {
				// ASN, SKI and Public key must exist
				if (asn == null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, ASN, null, ValidationErrorType.NULL));
				}
				if (ski == null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, SKI, null, ValidationErrorType.NULL));
				}
				if (publicKey == null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, PUBLIC_KEY, null, ValidationErrorType.NULL));
				}
			} else {
				validationErrors
						.add(new ValidationError(OBJECT_NAME, TYPE, type, ValidationErrorType.UNEXPECTED_VALUE));
			}
			// "It is RECOMMENDED that an explanatory comment is also included"
			// (draft-ietf-sidr-slurm-08)
			if (comment == null || comment.trim().isEmpty()) {
				validationErrors.add(new ValidationError(OBJECT_NAME, COMMENT, null, ValidationErrorType.NULL));
			} else if (!(comment.trim().length() > 0 && comment.trim().length() <= 2000)) {
				// MAX 2000 (randomly picked to avoid abuse)
				validationErrors.add(new ValidationError(OBJECT_NAME, COMMENT, comment,
						ValidationErrorType.LENGTH_OUT_OF_RANGE, 1, 2000));
			}
			if (ski != null) {
				try {
					Base64.getUrlDecoder().decode(ski);
				} catch (IllegalArgumentException e) {
					validationErrors
							.add(new ValidationError(OBJECT_NAME, SKI, ski, ValidationErrorType.UNEXPECTED_VALUE));
				}
			}
			if (publicKey != null) {
				try {
					Base64.getUrlDecoder().decode(publicKey);
				} catch (IllegalArgumentException e) {
					validationErrors.add(new ValidationError(OBJECT_NAME, PUBLIC_KEY, publicKey,
							ValidationErrorType.UNEXPECTED_VALUE));
				}
			}
		}
		if (!validationErrors.isEmpty()) {
			throw new ValidationException(validationErrors);
		}
	}

	private static String getTrimmedString(String value) {
		return value == null ? null : (value.trim().isEmpty() ? null : value.trim());
	}
}
