/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 1.4
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.productFlowThruDivision.states;

import com.qcadoo.mes.productFlowThruDivision.OperationComponentDataProvider;
import com.qcadoo.mes.productFlowThruDivision.constants.OperationProductInComponentFieldsPFTD;
import com.qcadoo.mes.productFlowThruDivision.constants.OperationProductOutComponentFieldsPFTD;
import com.qcadoo.mes.productFlowThruDivision.constants.Range;
import com.qcadoo.mes.productFlowThruDivision.constants.TechnologyFieldsPFTD;
import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.mes.states.constants.StateChangeStatus;
import com.qcadoo.mes.technologies.TechnologyService;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.mes.technologies.constants.TechnologyFields;
import com.qcadoo.mes.technologies.constants.TechnologyOperationComponentFields;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TechnologyStateValidationServicePFTD {

    private static final String L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING = "qcadooView.validate.field.error.missing";

    @Autowired
    private TechnologyService technologyService;

    @Autowired
    private OperationComponentDataProvider operationComponentDataProvider;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void validationOnAccepted(final StateChangeContext stateChangeContext) {
        final Entity technology = stateChangeContext.getOwner();
        if (technology != null && !stateChangeContext.getStatus().equals(StateChangeStatus.FAILURE)) {
            checkIfForOneDivisionLocationIsSet(technology, stateChangeContext);
            checkIfLocationInOperationIsSet(technology, stateChangeContext);
        }
    }

    private void checkIfLocationInOperationIsSet(Entity technology, StateChangeContext stateChangeContext) {
        checkForComponents(technology, stateChangeContext);
        checkForFinal(technology, stateChangeContext);
    }

    private void checkForComponents(Entity technology, StateChangeContext stateChangeContext) {
        List<Long> ids = operationComponentDataProvider.getComponentsForTechnology(technology.getId());
        if (ids.isEmpty()) {
            return;
        }
        List<Entity> opics = dataDefinitionService
                .get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_OPERATION_PRODUCT_IN_COMPONENT).find()
                .add(SearchRestrictions.in("id", ids)).list().getEntities();

        boolean valid = true;

        for (Entity op : opics) {
            if (op.getBelongsToField("componentsLocation") == null) {
                valid = false;
            }
        }
        if (valid) {
            return;
        }

        stateChangeContext.addValidationError("productFlowThruDivision.location.components.notFilled");

    }

    private void checkForFinal(Entity technology, StateChangeContext stateChangeContext) {
        List<Long> ids = operationComponentDataProvider.getFinalProductsForTechnology(technology.getId());
        if (ids.isEmpty()) {
            return;
        }
        List<Entity> opocs = dataDefinitionService
                .get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_OPERATION_PRODUCT_OUT_COMPONENT).find()
                .add(SearchRestrictions.in("id", ids)).list().getEntities();

        boolean valid = true;

        for (Entity op : opocs) {
            if (op.getBelongsToField("productsInputLocation") == null) {
                valid = false;
            }
        }
        if (valid) {
            return;
        }
        stateChangeContext.addValidationError("productFlowThruDivision.location.final.notFilled");

    }

    private void checkIfForOneDivisionLocationIsSet(final Entity technology, final StateChangeContext stateChangeContext) {
        if (technology.getField(TechnologyFieldsPFTD.RANGE).equals(Range.ONE_DIVISION.getStringValue())) {
            if (technology.getBelongsToField(TechnologyFieldsPFTD.COMPONENTS_LOCATION) == null) {
                stateChangeContext.addFieldValidationError(TechnologyFieldsPFTD.COMPONENTS_LOCATION,
                        L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);

            }

            if (technology.getBelongsToField(TechnologyFieldsPFTD.PRODUCTS_INPUT_LOCATION) == null) {
                stateChangeContext.addFieldValidationError(TechnologyFieldsPFTD.PRODUCTS_INPUT_LOCATION,
                        L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);
            }
        }
    }

    private void checkIfLocationOPICSet(final Entity technology, final StateChangeContext stateChangeContext) {
        for (Entity toc : technology.getHasManyField(TechnologyFields.OPERATION_COMPONENTS)) {
            for (Entity opic : toc.getHasManyField(TechnologyOperationComponentFields.OPERATION_PRODUCT_IN_COMPONENTS)) {
                if (!technologyService.isIntermediateProduct(opic)) {
                    if (opic.getBelongsToField(OperationProductInComponentFieldsPFTD.COMPONENTS_LOCATION) == null) {
                        stateChangeContext.addValidationError("productFlowThruDivision.states.validation.LocationOPICNotSet");
                        return;
                    }
                }
            }
        }
    }

    private void checkIfLocationOPOCSet(final Entity technology, final StateChangeContext stateChangeContext) {
        for (Entity toc : technology.getHasManyField(TechnologyFields.OPERATION_COMPONENTS)) {
            for (Entity opoc : toc.getHasManyField(TechnologyOperationComponentFields.OPERATION_PRODUCT_OUT_COMPONENTS)) {
                if (technologyService.isFinalProduct(opoc)) {
                    if (opoc.getBelongsToField(OperationProductOutComponentFieldsPFTD.PRODUCTS_INPUT_LOCATION) == null) {
                        stateChangeContext.addValidationError("productFlowThruDivision.states.validation.LocationOPOCNotSet");
                        return;
                    }
                }
            }
        }
    }
}
