package mx.nic.lab.rpki.sqlite.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.db.spi.RoaDAO;
import mx.nic.lab.rpki.sqlite.database.DatabaseSession;
import mx.nic.lab.rpki.sqlite.model.RoaModel;

/**
 * Implementation to retrieve ROAs data
 *
 */
public class RoaDAOImpl implements RoaDAO {

	@Override
	public Roa getById(Long id) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return RoaModel.getById(id, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

	@Override
	public List<Roa> getAll(int limit, int offset, LinkedHashMap<String, String> sort) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return RoaModel.getAll(limit, offset, sort, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

}
