/*
   Copyright 2012  Knowledge Media Institute - The Open University

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package uk.ac.open.kmi.iserve.discovery.api;

/**
 * Match Type Interface
 * 
 * @author Carlos Pedrinaci (Knowledge Media Institute - The Open University)
 */
public interface MatchType {

	public String getDescription();
	
	public String getShortName();
	
	public String getLongName();
	
	/**
	 * Get the ordinal value of this match type.
	 * This should be its position within the family of match types it belongs to.
	 * Should serve as a basis for comparison and basic scoring
	 * 
	 * @return
	 */
	public int ordinal();
	
}
