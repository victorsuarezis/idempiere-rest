package com.trekglobal.idempiere.rest.api.model;

import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MRefTable;
import org.compiere.model.MReference;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;

public class Custom_Column extends MColumn{



	public Custom_Column(Properties ctx, int AD_Column_ID, String trxName) {
		super(ctx, AD_Column_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;
	
	public String getReferenceTableName() {
		String foreignTable = null;
		int refid = getAD_Reference_ID();
		if (DisplayType.TableDir == refid || (DisplayType.Search == refid && getAD_Reference_Value_ID() == 0)) {
			foreignTable = getColumnName().substring(0, getColumnName().length()-3);
		} else if (DisplayType.Table == refid || DisplayType.Search == refid) {
			MReference ref = MReference.get(getCtx(), getAD_Reference_Value_ID(), get_TrxName());
			if (MReference.VALIDATIONTYPE_TableValidation.equals(ref.getValidationType())) {
				int cnt = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM AD_Ref_Table WHERE AD_Reference_ID=?", getAD_Reference_Value_ID());
				if (cnt == 1) {
					MRefTable rt = MRefTable.get(getCtx(), getAD_Reference_Value_ID(), get_TrxName());
					if (rt != null) {
						MTable table = MTable.get(getCtx(), rt.getAD_Table_ID(), get_TrxName());
						if (table == null) {
							throw new AdempiereException("Table " + rt.getAD_Table_ID() + " not found");
						}
						foreignTable = table.getTableName();
					}
				}
			}
		} else if (DisplayType.Button == refid) {
			// C_BPartner.AD_OrgBP_ID and C_Project.C_ProjectType_ID are defined as buttons
			if ("AD_OrgBP_ID".equalsIgnoreCase(getColumnName()))
				foreignTable = "AD_Org";
			else if ("C_ProjectType_ID".equalsIgnoreCase(getColumnName()))
				foreignTable = "C_ProjectType";
		} else if (DisplayType.isList(refid)) {
			foreignTable = "AD_Ref_List";
		} else if (DisplayType.Location == refid || "LocationExtended".equals( getAD_Reference().getName())) {
			foreignTable = "C_Location";
		} else if (DisplayType.Account == refid) {
			foreignTable = "C_ValidCombination";
		} else if (DisplayType.Locator == refid) {
			foreignTable = "M_Locator";
		} else if (DisplayType.PAttribute == refid) {
			foreignTable = "M_AttributeSetInstance";
		} else if (DisplayType.Assignment == refid) {
			foreignTable = "S_ResourceAssignment";
		} else if (DisplayType.Image == refid && !"BinaryData".equals(getColumnName())) {
			foreignTable = "AD_Image";
		} else if (DisplayType.Chart == refid) {
			foreignTable = "AD_Chart";
		}

		if (foreignTable != null) {
			if (foreignTable.equals("AD_AllClients_V")) {
				foreignTable = "AD_Client";
			} else if (foreignTable.equals("AD_AllUsers_V")) {
				foreignTable = "AD_User";
			} else if (foreignTable.equals("AD_AllRoles_V")) {
				foreignTable = "AD_Role";
			}
		}

		return foreignTable;
	}

}
