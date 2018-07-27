package mx.nic.lab.rpki.sqlite.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.spi.SlurmBgpsecDAO;
import mx.nic.lab.rpki.sqlite.database.DatabaseSession;
import mx.nic.lab.rpki.sqlite.model.SlurmBgpsecModel;

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
	public List<SlurmBgpsec> getAll(int id) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return SlurmBgpsecModel.getAll(connection, id);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

}
