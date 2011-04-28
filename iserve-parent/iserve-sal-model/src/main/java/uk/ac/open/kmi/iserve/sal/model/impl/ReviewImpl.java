/*
   Copyright ${year}  Knowledge Media Institute - The Open University

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
package uk.ac.open.kmi.iserve.sal.model.impl;

import java.util.Date;

import uk.ac.open.kmi.iserve.sal.model.common.URI;
import uk.ac.open.kmi.iserve.sal.model.review.Review;

public class ReviewImpl implements Review {

	private static final long serialVersionUID = -744404352292380833L;

	protected Date createTime;

	protected URI reviewerUri;

	public ReviewImpl() {
		setReviewerUri(null);
		setCreateTime(null);
	}

	public ReviewImpl(URI reviewerUri, Date createTime) {
		setReviewerUri(reviewerUri);
		setCreateTime(createTime);
	}

	public Date getCreateTime() {
		return createTime;
	}

	public URI getReviewerUri() {
		return reviewerUri;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public void setReviewerUri(URI reviewerUri) {
		this.reviewerUri = reviewerUri;		
	}

}
