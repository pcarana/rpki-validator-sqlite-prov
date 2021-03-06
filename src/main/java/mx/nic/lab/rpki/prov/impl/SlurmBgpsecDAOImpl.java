package mx.nic.lab.rpki.prov.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ValidationError;
import mx.nic.lab.rpki.db.exception.ValidationErrorType;
import mx.nic.lab.rpki.db.exception.ValidationException;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.spi.SlurmBgpsecDAO;
import mx.nic.lab.rpki.prov.database.DatabaseSession;
import mx.nic.lab.rpki.prov.model.SlurmBgpsecModel;
import mx.nic.lab.rpki.prov.object.SlurmBgpsecDbObject;
import mx.nic.lab.rpki.prov.object.DatabaseObject.Operation;

/**
 * Implementation to retrieve SLURM BGPsec data
 *
 */
public class SlurmBgpsecDAOImpl implements SlurmBgpsecDAO {

	@Override
	public SlurmBgpsec getById(Long id) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return SlurmBgpsecModel.getById(id, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public ListResult<SlurmBgpsec> getAll(PagingParameters pagingParams) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return SlurmBgpsecModel.getAll(pagingParams, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public ListResult<SlurmBgpsec> getAllByType(String type, PagingParameters pagingParams)
			throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return SlurmBgpsecModel.getAllByType(type, pagingParams, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public boolean create(SlurmBgpsec newSlurmBgpsec) throws ApiDataAccessException {
		SlurmBgpsecDbObject slurmBgpsecDb = new SlurmBgpsecDbObject(newSlurmBgpsec);
		slurmBgpsecDb.validate(Operation.CREATE);

		try (Connection connection = DatabaseSession.getConnection()) {
			// Validate that the object doesn't exists
			if (SlurmBgpsecModel.exist(newSlurmBgpsec, connection)) {
				throw new ValidationException(
						new ValidationError(SlurmBgpsec.OBJECT_NAME, ValidationErrorType.OBJECT_EXISTS));
			}
			return SlurmBgpsecModel.create(newSlurmBgpsec, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public boolean deleteById(Long id) throws ApiDataAccessException {
		// First check that the object actually exists
		try (Connection connection = DatabaseSession.getConnection()) {
			SlurmBgpsec bgpsec = SlurmBgpsecModel.getById(id, connection);
			if (bgpsec == null) {
				throw new ValidationException(
						new ValidationError(SlurmBgpsec.OBJECT_NAME, ValidationErrorType.OBJECT_NOT_EXISTS));
			}
			int deleted = SlurmBgpsecModel.deleteById(id, connection);
			return deleted > 0;
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public SlurmBgpsec getBgpsecByProperties(Long asn, String ski, String routerPublicKey, String type)
			throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return SlurmBgpsecModel.getByProperties(asn, ski, routerPublicKey, type, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public int updateComment(Long id, String newComment) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			// Validate that the object exists
			if (SlurmBgpsecModel.getById(id, connection) == null) {
				throw new ValidationException(
						new ValidationError(SlurmBgpsec.OBJECT_NAME, ValidationErrorType.OBJECT_NOT_EXISTS));
			}
			return SlurmBgpsecModel.updateComment(id, newComment, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public int updateOrder(Long id, int newOrder) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			// Validate that the object exists
			if (SlurmBgpsecModel.getById(id, connection) == null) {
				throw new ValidationException(
						new ValidationError(SlurmBgpsec.OBJECT_NAME, ValidationErrorType.OBJECT_NOT_EXISTS));
			}
			return SlurmBgpsecModel.updateOrder(id, newOrder, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public void bulkDelete(Set<Long> ids) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			SlurmBgpsecModel.bulkDelete(ids, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}
}
