/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.algo.tdidt;

import java.io.IOException;

import clus.main.*;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.pruning.*;
import clus.util.*;
import clus.algo.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.*;

import jeans.util.cmdline.*;

public class ClusDecisionTree extends ClusInductionAlgorithmType {

	public final static int LEVEL_WISE = 0;
	public final static int DEPTH_FIRST = 1;

	public ClusDecisionTree(Clus clus) {
		super(clus);
	}

	public void printInfo() {
		System.out.println("TDIDT");
		System.out.println("Heuristic: "+getStatManager().getHeuristicName());
	}

        // ********************************
        // PBCT-HMC
        // author: @zamith
	public ClusInductionAlgorithm createInduce(ClusSchema schema, ClusSchema verticalSchema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		if (sett.hasConstraintFile()) {
			boolean fillin = cargs.hasOption("fillin");
			return new ConstraintDFInduce(schema, sett, fillin);              
                // ********************************
                //PBCT: Use LookAhead induction, only implemented for the PBCT algorithm
                } else if(sett.getIsPBCT()) {
                        return new LookAheadPBCT(schema,verticalSchema,sett);
                // ********************************
		} else {
			if(sett.checkInductionOrder("DepthFirst")){
				return new DepthFirstInduce(schema, sett);
                        }
		}
            return null;
	}
        // ********************************

}
