package com.trekglobal.idempiere.rest.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.I_C_BPartner_Location;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MColumn;
import org.compiere.model.MLocation;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.model.POInfoColumn;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.ValueNamePair;

public class Custom_PartnerLocation extends MBPartnerLocation{
	
	private static CCache<Integer,List<ValueNamePair>> fks_cache	= new CCache<Integer,List<ValueNamePair>>("FKs", 5);


	public Custom_PartnerLocation(Properties ctx, int C_Location_ID, String trxName) {
		super(ctx, C_Location_ID, trxName);
	}

	private static final long serialVersionUID = 1L;
	
	public boolean validForeignKeys() {
		List<ValueNamePair> fks = getForeignColumnIdxs();
		if (fks == null) {
			return true;
		}
		for (ValueNamePair vnp : fks) {
			String fkcol = vnp.getID();
			String fktab = vnp.getName();
			int index = get_ColumnIndex(fkcol); 
			if (is_new() || is_ValueChanged(index)) {
				int fkval = get_ValueAsInt(index);
				if (fkval > 0) {
					MTable ft = MTable.get(getCtx(), fktab);
					boolean systemAccess = false;
					String accessLevel = ft.getAccessLevel();
					if (   MTable.ACCESSLEVEL_All.equals(accessLevel)
						|| MTable.ACCESSLEVEL_SystemOnly.equals(accessLevel)
						|| MTable.ACCESSLEVEL_SystemPlusClient.equals(accessLevel)) {
						systemAccess = true;
					}
					StringBuilder sql = new StringBuilder("SELECT AD_Client_ID FROM ")
							.append(fktab)
							.append(" WHERE ")
							.append(ft.getKeyColumns()[0])
							.append("=?");
					int pocid = DB.getSQLValue(get_TrxName(), sql.toString(), fkval);
					if (pocid < 0) {
						log.saveError("Error", "Foreign ID " + fkval + " not found in " + fkcol);
						return false;
					}
					if (pocid == 0 && !systemAccess) {
						log.saveError("Error", "System ID " + fkval + " cannot be used in " + fkcol);
						return false;
					}
					int curcid = Env.getAD_Client_ID(getCtx());
					if (pocid > 0 && pocid != curcid) {
						log.saveError("Error", "Cross tenant ID " + fkval + " not allowed in " + fkcol);
						return false;
					}
				}
			}
		}
		return true;
	}

	private List<ValueNamePair> getForeignColumnIdxs() {
		List<ValueNamePair> retValue;
		if (fks_cache.containsKey(get_Table_ID())) {
			retValue = fks_cache.get(get_Table_ID());
			return retValue;
		}
		
		retValue = new ArrayList<ValueNamePair>();
		
		List<MColumn> lstColumns = new Query(Env.getCtx(), MColumn.Table_Name, "AD_Table_ID = ?", get_TrxName()).
				setParameters(MTable.getTable_ID(I_C_BPartner_Location.Table_Name)).setOnlyActiveRecords(true).list();
		
		for (MColumn mColumn : lstColumns) {
			if ("C_Location_ID".equals(mColumn.getColumnName()))
				System.out.println("");
			Custom_Column c_column = new Custom_Column(getCtx(), mColumn.get_ID(), mColumn.get_TrxName());
			int dt = c_column.getAD_Reference_ID();
			if (dt != DisplayType.ID && DisplayType.isID(dt)) {
				if ("AD_Client_ID".equals(c_column.getColumnName())) {
					// ad_client_id is verified with checkValidClient
					continue;
				}
				String refTable = c_column.getReferenceTableName();
				retValue.add(new ValueNamePair(c_column.getColumnName(), refTable));
			}
		}
		

		if (retValue.size() == 0) {
			retValue = null;
		}
		fks_cache.put(get_Table_ID(), retValue);
		return retValue;
	}
	
}
