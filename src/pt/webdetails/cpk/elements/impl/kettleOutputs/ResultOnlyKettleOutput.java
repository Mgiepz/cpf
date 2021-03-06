/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package pt.webdetails.cpk.elements.impl.kettleOutputs;

import java.util.Map;
import org.pentaho.platform.api.engine.IParameterProvider;

/**
 *
 * @author Pedro Alves<pedro.alves@webdetails.pt>
 */
public class ResultOnlyKettleOutput extends KettleOutput {

    public ResultOnlyKettleOutput(Map<String, IParameterProvider> parameterProviders) {
        super(parameterProviders);
    }

    @Override
    public boolean needsRowListener() {
        return false;
    }

    @Override
    public void processResult() {
        super.processResultOnly();
    }
}
