package mx.nic.lab.rpki.prov.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

import mx.nic.lab.rpki.db.exception.ValidationError;
import mx.nic.lab.rpki.db.exception.ValidationErrorType;
import mx.nic.lab.rpki.db.exception.ValidationException;
import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;

/**
 * Extension of {@link SlurmBgpsec} as a {@link DatabaseObject}
 *
 */
public class SlurmBgpsecDbObject extends SlurmBgpsec implements DatabaseObject {

	public static final String ID_COLUMN = "slb_id";
	public static final String ASN_COLUMN = "slb_asn";
	public static final String SKI_COLUMN = "slb_ski";
	public static final String ROUTER_PUBLIC_KEY_COLUMN = "slb_public_key";
	public static final String TYPE_COLUMN = "slb_type";
	public static final String COMMENT_COLUMN = "slb_comment";
	public static final String ORDER_COLUMN = "slb_order";

	public static final int COMMENT_MIN_LENGTH = 1;
	public static final int COMMENT_MAX_LENGTH = 2000;

	/**
	 * Mapping of the {@link SlurmBgpsec} properties to its corresponding DB column
	 */
	public static final Map<String, String> propertyToColumnMap;
	static {
		propertyToColumnMap = new HashMap<>();
		propertyToColumnMap.put(ID, ID_COLUMN);
		propertyToColumnMap.put(ASN, ASN_COLUMN);
		propertyToColumnMap.put(SKI, SKI_COLUMN);
		propertyToColumnMap.put(ROUTER_PUBLIC_KEY, ROUTER_PUBLIC_KEY_COLUMN);
		propertyToColumnMap.put(TYPE, TYPE_COLUMN);
		propertyToColumnMap.put(COMMENT, COMMENT_COLUMN);
		propertyToColumnMap.put(ORDER, ORDER_COLUMN);
	}

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
		this.setRouterPublicKey(slurmBgpsec.getRouterPublicKey());
		this.setType(slurmBgpsec.getType());
		this.setComment(slurmBgpsec.getComment());
		this.setOrder(slurmBgpsec.getOrder());
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
		setRouterPublicKey(resultSet.getString(ROUTER_PUBLIC_KEY_COLUMN));
		setType(resultSet.getString(TYPE_COLUMN));
		setComment(resultSet.getString(COMMENT_COLUMN));
		setOrder(resultSet.getInt(ORDER_COLUMN));
		if (resultSet.wasNull()) {
			setOrder(null);
		}
	}

	@Override
	public void storeToDatabase(PreparedStatement statement) throws SQLException {
		if (getAsn() != null) {
			statement.setLong(1, getAsn());
		} else {
			statement.setNull(1, Types.NUMERIC);
		}
		if (getSki() != null) {
			statement.setString(2, getSki());
		} else {
			statement.setNull(2, Types.VARCHAR);
		}
		if (getRouterPublicKey() != null) {
			statement.setString(3, getRouterPublicKey());
		} else {
			statement.setNull(3, Types.VARCHAR);
		}
		if (getType() != null) {
			statement.setString(4, getType());
		} else {
			statement.setNull(4, Types.VARCHAR);
		}
		if (getComment() != null) {
			statement.setString(5, getComment());
		} else {
			statement.setNull(5, Types.VARCHAR);
		}
		if (getOrder() != null) {
			statement.setInt(6, getOrder());
		} else {
			statement.setNull(6, Types.INTEGER);
		}
	}

	@Override
	public void validate(Operation operation) throws ValidationException {
		List<ValidationError> validationErrors = new ArrayList<>();
		if (operation == Operation.CREATE) {
			// Check the attributes according to the type
			// The ID isn't validated since this is a new object
			String type = this.getType();
			Long asn = this.getAsn();
			String ski = getTrimmedString(this.getSki());
			String routerPublicKey = getTrimmedString(this.getRouterPublicKey());
			String comment = getTrimmedString(this.getComment());

			if (type == null) {
				validationErrors.add(new ValidationError(OBJECT_NAME, TYPE, null, ValidationErrorType.NULL));
				// The property must be present to keep validating
				throw new ValidationException(validationErrors);
			} else if (type.equals(TYPE_FILTER)) {
				// Either an ASN or a SKI must exist
				if (asn == null && ski == null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, ASN, null, ValidationErrorType.NULL));
					validationErrors.add(new ValidationError(OBJECT_NAME, SKI, null, ValidationErrorType.NULL));
				}
				// Router's Public key is only for assertions
				if (routerPublicKey != null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, ROUTER_PUBLIC_KEY, routerPublicKey,
							ValidationErrorType.NOT_NULL));
				}
			} else if (type.equals(TYPE_ASSERTION)) {
				// ASN, SKI and Public key must exist
				if (asn == null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, ASN, null, ValidationErrorType.NULL));
				}
				if (ski == null) {
					validationErrors.add(new ValidationError(OBJECT_NAME, SKI, null, ValidationErrorType.NULL));
				}
				if (routerPublicKey == null) {
					validationErrors
							.add(new ValidationError(OBJECT_NAME, ROUTER_PUBLIC_KEY, null, ValidationErrorType.NULL));
				}
			} else {
				validationErrors
						.add(new ValidationError(OBJECT_NAME, TYPE, type, ValidationErrorType.UNEXPECTED_VALUE));
			}
			if (asn != null) {
				if (asn < ApiObject.ASN_MIN_VALUE || asn > ApiObject.ASN_MAX_VALUE) {
					validationErrors.add(new ValidationError(OBJECT_NAME, ASN, asn,
							ValidationErrorType.VALUE_OUT_OF_RANGE, ApiObject.ASN_MIN_VALUE, ApiObject.ASN_MAX_VALUE));
				}
			}
			// "It is RECOMMENDED that an explanatory comment is also included" (RFC 8416)
			// but leave it optional
			if (comment != null && !(comment.trim().length() >= COMMENT_MIN_LENGTH
					&& comment.trim().length() <= COMMENT_MAX_LENGTH)) {
				validationErrors.add(new ValidationError(OBJECT_NAME, COMMENT, comment,
						ValidationErrorType.LENGTH_OUT_OF_RANGE, COMMENT_MIN_LENGTH, COMMENT_MAX_LENGTH));
			}
			if (ski != null) {
				try {
					byte[] decodedSki = Base64.getDecoder().decode(ski);
					byte[] hexBytes = Hex.decode(decodedSki);
					// Is the 160-bit SHA-1 hash (RFC 8416 section 3.3.2 citing RFC 6487 section
					// 4.8.2)
					if (hexBytes.length != 20) {
						throw new IllegalArgumentException();
					}
				} catch (IllegalArgumentException e) {
					validationErrors
							.add(new ValidationError(OBJECT_NAME, SKI, ski, ValidationErrorType.UNEXPECTED_VALUE));
				} catch (DecoderException e) {
					validationErrors
							.add(new ValidationError(OBJECT_NAME, SKI, ski, ValidationErrorType.UNEXPECTED_VALUE));
				}
			}
			if (routerPublicKey != null) {
				try {
					Base64.getDecoder().decode(routerPublicKey);
				} catch (IllegalArgumentException e) {
					validationErrors.add(new ValidationError(OBJECT_NAME, ROUTER_PUBLIC_KEY, routerPublicKey,
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
