/*
 * Copyright (c) 2015-17 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.genome_nexus.annotation.web;

import io.swagger.annotations.*;
import org.cbioportal.genome_nexus.annotation.domain.*;
import org.cbioportal.genome_nexus.annotation.service.internal.*;
import org.cbioportal.genome_nexus.annotation.service.*;

import org.cbioportal.genome_nexus.annotation.web.config.PublicApi;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import org.apache.commons.logging.*;

/**
 * @author Benjamin Gross
 */
@PublicApi
@RestController // shorthand for @Controller, @ResponseBody
@CrossOrigin(origins="*") // allow all cross-domain requests
@RequestMapping(value= "/")
public class AnnotationController
{
    private final VariantAnnotator variantAnnotator;
    private final IsoformOverrideService isoformOverrideService;
    private final HotspotService hotspotService;
    private final MutationAssessorService mutationAssessorService;

    private static final Log LOG = LogFactory.getLog(AnnotationController.class);

    @Autowired
    public AnnotationController(VariantAnnotator variantAnnotator,
                                IsoformOverrideService isoformOverrideService,
                                HotspotService hotspotService,
                                MutationAssessorService mutationService)
    {
        this.variantAnnotator = variantAnnotator;
        this.isoformOverrideService = isoformOverrideService;
        this.hotspotService = hotspotService;
        this.mutationAssessorService = mutationService;
    }

    // TODO remove this endpoint after all internal dependencies are resolved
    @ApiOperation(value = "Retrieves VEP annotation for the provided list of variants",
        hidden = true,
        nickname = "getVariantAnnotation")
	@RequestMapping(value = "/hgvs/{variants:.+}",
        method = RequestMethod.GET,
        produces = "application/json")
    @Deprecated
	public List<VariantAnnotation> getVariantAnnotation(
        @PathVariable
        @ApiParam(value="Comma separated list of variants. For example X:g.66937331T>A,17:g.41242962->GA",
            required = true,
            allowMultiple = true)
        List<String> variants,
        @RequestParam(required = false)
        @ApiParam(value="Isoform override source. For example uniprot",
            required = false)
        String isoformOverrideSource,
        @RequestParam(required = false)
        @ApiParam(value="Comma separated list of fields to include (case-sensitive!). " +
            "For example: hotspots,mutation_assessor", required = false, defaultValue = "hotspots,mutation_assessor")
        List<String> fields)
    {
        return this.fetchVariantAnnotationPOST(variants, isoformOverrideSource, fields);
    }

    // TODO remove this endpoint after all internal dependencies are resolved
    @ApiOperation(value = "Retrieves VEP annotation for the provided list of variants",
        hidden = true,
        nickname = "postVariantAnnotation")
    @RequestMapping(value = "/hgvs",
        method = RequestMethod.POST,
        produces = "application/json")
    @Deprecated
    public List<VariantAnnotation> postVariantAnnotation(
        @RequestParam
        @ApiParam(value="Comma separated list of variants. For example X:g.66937331T>A,17:g.41242962->GA",
            required = true,
            allowMultiple = true)
            List<String> variants,
        @RequestParam(required = false)
        @ApiParam(value="Isoform override source. For example uniprot",
            required = false)
            String isoformOverrideSource,
        @RequestParam(required = false)
        @ApiParam(value="Comma separated list of fields to include (case-sensitive!). " +
            "For example: hotspots,mutation_assessor", required = false, defaultValue = "hotspots,mutation_assessor")
            List<String> fields)
    {
        return fetchVariantAnnotationPOST(variants, isoformOverrideSource, fields);
    }

    @ApiOperation(value = "Retrieves VEP annotation for the provided list of variants",
        nickname = "fetchVariantAnnotationPOST")
    @RequestMapping(value = "/annotation",
        method = RequestMethod.POST,
        produces = "application/json")
    public List<VariantAnnotation> fetchVariantAnnotationPOST(
        @ApiParam(value="List of variants. For example [\"X:g.66937331T>A\",\"17:g.41242962->GA\"]",
            required = true)
        @RequestBody List<String> variants,
        @ApiParam(value="Isoform override source. For example uniprot",
            required = false)
        @RequestParam(required = false) String isoformOverrideSource,
        @ApiParam(value="Comma separated list of fields to include (case-sensitive!). " +
            "For example: hotspots,mutation_assessor", required = false, defaultValue = "hotspots,mutation_assessor")
        @RequestParam(required = false) List<String> fields)
    {
        EnrichmentService postEnrichmentService = this.initPostEnrichmentService(isoformOverrideSource, fields);

        return this.variantAnnotator.getVariantAnnotations(variants, postEnrichmentService);
	}

    @ApiOperation(value = "Retrieves VEP annotation for the provided variant",
        nickname = "fetchVariantAnnotationGET")
    @RequestMapping(value = "/annotation/{variant:.+}",
        method = RequestMethod.GET,
        produces = "application/json")
    public VariantAnnotation fetchVariantAnnotationGET(
        @ApiParam(value="Variant. For example 17:g.41242962->GA",
            required = true)
        @PathVariable String variant,
        @ApiParam(value="Isoform override source. For example uniprot",
            required = false)
        @RequestParam(required = false) String isoformOverrideSource,
        @ApiParam(value="Comma separated list of fields to include (case-sensitive!). " +
            "For example: hotspots,mutation_assessor", required = false, defaultValue = "hotspots,mutation_assessor")
        @RequestParam(required = false) List<String> fields)
    {
        EnrichmentService postEnrichmentService = this.initPostEnrichmentService(isoformOverrideSource, fields);

        return this.variantAnnotator.getVariantAnnotation(variant, postEnrichmentService);
    }

    private EnrichmentService initPostEnrichmentService(String isoformOverrideSource, List<String> fields)
    {
        // The post enrichment service enriches the annotation after saving
        // the original annotation data to the repository. Any enrichment
        // performed by the post enrichment service is not saved
        // to the annotation repository.
        EnrichmentService postEnrichmentService = new VEPEnrichmentService();

        // only register the enricher if the service actually has data for the given source
        if (isoformOverrideService.hasData(isoformOverrideSource))
        {
            AnnotationEnricher enricher = new IsoformAnnotationEnricher(
                isoformOverrideSource, isoformOverrideService);

            postEnrichmentService.registerEnricher(isoformOverrideSource, enricher);
        }

        if (fields != null && fields.contains("hotspots"))
        {
            AnnotationEnricher enricher = new HotspotAnnotationEnricher(hotspotService, true);
            postEnrichmentService.registerEnricher("cancerHotspots", enricher);
        }
        if (fields != null && fields.contains("mutation_assessor"))
        {
            AnnotationEnricher enricher = new MutationAssessorAnnotationEnricher(mutationAssessorService);
            postEnrichmentService.registerEnricher("mutation_assessor", enricher);
        }

        return postEnrichmentService;
    }
}