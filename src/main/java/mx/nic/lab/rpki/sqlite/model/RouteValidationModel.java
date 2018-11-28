package mx.nic.lab.rpki.sqlite.model;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.db.pojo.RouteValidation;
import mx.nic.lab.rpki.db.pojo.RouteValidation.AsState;
import mx.nic.lab.rpki.db.pojo.RouteValidation.PrefixState;
import mx.nic.lab.rpki.db.pojo.RouteValidation.ValidityState;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;

/**
 * Model to validate a route fetching data from the database (ROAs) and applying
 * some logic independent from the database. This class uses {@link RoaModel}
 * and {@link SlurmPrefixModel}.
 *
 */
public class RouteValidationModel {

	/**
	 * Validates the route with the received parameters and return the validation
	 * state following the RFC 6483 section-2 indications and the configured SLURM.
	 * 
	 * @param asn
	 * @param prefix
	 * @param prefixLength
	 * @param connection
	 * @return The {@link RouteValidation} with the result of the validation
	 * @throws SQLException
	 */
	public static RouteValidation validate(Long asn, byte[] prefix, Integer prefixLength, Connection connection)
			throws SQLException {
		// If there's an assertion then stop the search and return the assertion result
		RouteValidation slurmValidation = findSlurmAssertion(asn, prefix, prefixLength, connection);
		if (slurmValidation != null) {
			return slurmValidation;
		}
		// No assertion, check if there's a filter
		slurmValidation = findSlurmFilter(asn, prefix, prefixLength, connection);
		if (slurmValidation != null) {
			return slurmValidation;
		}
		// Well, then go for the ROA match(es)
		return findRoaValidation(asn, prefix, prefixLength, connection);

	}

	/**
	 * Look for a SLURM assertion that matches the prefix, the SLURM assertion is
	 * treated just as a valid ROA
	 * 
	 * @param asn
	 * @param prefix
	 * @param prefixLength
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	private static RouteValidation findSlurmAssertion(Long asn, byte[] prefix, Integer prefixLength,
			Connection connection) throws SQLException {
		// Go for the exact SLURM prefix assertion match
		SlurmPrefix matchedSlurmPrefix = SlurmPrefixModel.findExactMatch(prefix, prefixLength, connection);
		if (matchedSlurmPrefix != null) {
			boolean asnMatch = asn.equals(matchedSlurmPrefix.getAsn());
			ValidityState validityState = asnMatch ? ValidityState.VALID : ValidityState.INVALID;
			return createSlurmRouteValidation(validityState, PrefixState.MATCH_ROA, asnMatch, matchedSlurmPrefix);
		}
		// Check if there's a SLURM prefix assertion covering the received prefix (a.k.a
		// the
		// received prefix is more specific than SLURM prefix)
		List<SlurmPrefix> candidatePrefixes = SlurmPrefixModel.findCoveringAggregate(prefix, prefixLength, connection);
		for (SlurmPrefix slurmPrefix : candidatePrefixes) {
			// The prefix is effectively a son of the SLURM prefix
			if (!isPrefixInRange(prefix, slurmPrefix.getStartPrefix(), slurmPrefix.getPrefixLength())) {
				continue;
			}
			return createSlurmRouteValidation(ValidityState.INVALID, PrefixState.MORE_SPECIFIC,
					asn.equals(slurmPrefix.getAsn()), slurmPrefix);
		}
		// Check if there's a SLURM prefix more specific (a.k.a the received prefix is a
		// covering aggregate of the SLURM prefix)
		candidatePrefixes = SlurmPrefixModel.findMoreSpecific(prefix, prefixLength, connection);
		for (SlurmPrefix slurmPrefix : candidatePrefixes) {
			// The SLURM prefix is effectively a son of the prefix
			if (!isPrefixInRange(slurmPrefix.getStartPrefix(), prefix, prefixLength)) {
				continue;
			}
			return createSlurmRouteValidation(ValidityState.UNKNOWN, PrefixState.COVERING_AGGREGATE,
					asn.equals(slurmPrefix.getAsn()), slurmPrefix);
		}
		// There's no "UNKNOWN" case for SLURM assertions, return null to search for
		// real ROAs
		return null;
	}

	/**
	 * Check if there's a SLURM filter that "filters" the received prefix
	 * 
	 * @param asn
	 * @param prefix
	 * @param prefixLength
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	private static RouteValidation findSlurmFilter(Long asn, byte[] prefix, Integer prefixLength, Connection connection)
			throws SQLException {
		// Search if there's any filter that matches the request
		SlurmPrefix matchedFilter = SlurmPrefixModel.findFilterMatch(asn, prefix, prefixLength, connection);
		if (matchedFilter != null) {
			return createSlurmRouteValidation(ValidityState.UNKNOWN, PrefixState.NON_INTERSECTING, false,
					matchedFilter);
		}
		return null;
	}

	/**
	 * Find if there's a ROA that matches the received prefix
	 * 
	 * @param asn
	 * @param prefix
	 * @param prefixLength
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	private static RouteValidation findRoaValidation(Long asn, byte[] prefix, Integer prefixLength,
			Connection connection) throws SQLException {
		// Go for the exact ROA match
		Roa matchedRoa = RoaModel.findExactMatch(prefix, prefixLength, connection);
		if (matchedRoa != null) {
			boolean asnMatch = asn.equals(matchedRoa.getAsn());
			ValidityState validityState = asnMatch ? ValidityState.VALID : ValidityState.INVALID;
			return createRoaRouteValidation(validityState, PrefixState.MATCH_ROA, asnMatch, matchedRoa);
		}
		// Check if there's a ROA covering the received prefix (a.k.a the received
		// prefix is more specific than ROA)
		List<Roa> candidateRoas = RoaModel.findCoveringAggregate(prefix, prefixLength, connection);
		for (Roa roa : candidateRoas) {
			// The prefix is effectively a son of the ROA
			if (!isPrefixInRange(prefix, roa.getStartPrefix(), roa.getPrefixLength())) {
				continue;
			}
			return createRoaRouteValidation(ValidityState.INVALID, PrefixState.MORE_SPECIFIC, asn.equals(roa.getAsn()),
					roa);
		}
		// Check if there's a ROA more specific (a.k.a the received prefix is a
		// covering aggregate of the ROA)
		candidateRoas = RoaModel.findMoreSpecific(prefix, prefixLength, connection);
		for (Roa roa : candidateRoas) {
			// The ROA is effectively a son of the prefix
			if (!isPrefixInRange(roa.getStartPrefix(), prefix, prefixLength)) {
				continue;
			}
			return createRoaRouteValidation(ValidityState.UNKNOWN, PrefixState.COVERING_AGGREGATE,
					asn.equals(roa.getAsn()), roa);
		}
		// No match at all, check if at least oen ROA exists with the ASN
		return createRoaRouteValidation(ValidityState.UNKNOWN, PrefixState.NON_INTERSECTING,
				RoaModel.existAsn(asn, connection), null);
	}

	/**
	 * Check if the <code>sonPrefix</code> is the same range that the
	 * <code>fatherPrefix</code>, using the prefix length of the father
	 * (<code>fatherLength</code>).
	 * 
	 * @param sonPrefix
	 * @param fatherPrefix
	 * @param fatherLength
	 * @return
	 */
	private static boolean isPrefixInRange(byte[] sonPrefix, byte[] fatherPrefix, Integer fatherLength) {
		// Both prefix are of the same IP type
		if (sonPrefix.length != fatherPrefix.length) {
			return false;
		}
		int bytesBase = fatherLength / 8;
		int bitsBase = fatherLength % 8;
		byte[] prefixLengthMask = new byte[fatherPrefix.length];
		int currByte = 0;
		for (; currByte < bytesBase; currByte++) {
			prefixLengthMask[currByte] |= 255;
		}
		if (currByte < prefixLengthMask.length) {
			prefixLengthMask[currByte] = (byte) (255 << (8 - bitsBase));
		}
		BigInteger sonIp = new BigInteger(sonPrefix);
		BigInteger fatherIp = new BigInteger(fatherPrefix);
		BigInteger mask = new BigInteger(prefixLengthMask);
		return sonIp.and(mask).equals(fatherIp);
	}

	/**
	 * Create a new instance of {@link RouteValidation} with the specified values,
	 * considering that the object matches is a {@link SlurmPrefix}; use the
	 * <code>asnMatch<code> to determine the {@link AsState}.
	 * 
	 * @param validityState
	 * @param prefixState
	 * @param asnMatch
	 * @param matchRoa
	 * @return
	 */
	private static RouteValidation createRoaRouteValidation(ValidityState validityState, PrefixState prefixState,
			boolean asnMatch, Roa matchRoa) {
		RouteValidation result = new RouteValidation();
		result.setValidityState(validityState);
		result.setPrefixState(prefixState);
		result.setAsState(asnMatch ? AsState.MATCHING : AsState.NON_MATCHING);
		result.setRoaMatch(matchRoa);
		return result;
	}

	/**
	 * Create a new instance of {@link RouteValidation} with the specified values,
	 * considering that the object matches is a {@link SlurmPrefix}; use the
	 * <code>asnMatch<code> to determine the {@link AsState}.
	 * 
	 * @param validityState
	 * @param prefixState
	 * @param asnMatch
	 * @param matchSlurmPrefix
	 * @return
	 */
	private static RouteValidation createSlurmRouteValidation(ValidityState validityState, PrefixState prefixState,
			boolean asnMatch, SlurmPrefix matchSlurmPrefix) {
		RouteValidation result = new RouteValidation();
		result.setValidityState(validityState);
		result.setPrefixState(prefixState);
		result.setAsState(asnMatch ? AsState.MATCHING : AsState.NON_MATCHING);
		result.setSlurmMatch(matchSlurmPrefix);
		return result;
	}
}
