package co.edu.javeriana.dsbp.proyecto.integration;

import co.edu.javeriana.dsbp.proyecto.integration.aggregators.SourceArticlesAggregator;
import co.edu.javeriana.dsbp.proyecto.model.ebi.ResponseWrapper;
import co.edu.javeriana.dsbp.proyecto.utils.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JacksonXMLDataFormat;
import org.springframework.stereotype.Component;

@Component
public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        JacksonXMLDataFormat df = new JacksonXMLDataFormat();
        df.setUnmarshalType(ResponseWrapper.class);

        from("direct:ebi-proxy")
                .id("ebiProxyRoute")
                .setProperty("query", simple("${body}"))
                .setBody(constant(null))
                .removeHeaders("*")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader(Exchange.HTTP_QUERY, simple("query=${exchangeProperty[query]}"))
                .log(LoggingLevel.INFO, "Searching articles in ebi")
                .to("https://www.ebi.ac.uk/europepmc/webservices/rest/search")
                .convertBodyTo(String.class)
                .log(LoggingLevel.DEBUG, "Antes del unmarshal ${body}")
                .unmarshal(df)
                .bean(Converter.class)
                .log(LoggingLevel.DEBUG, "Despues del unmarshal ${body}");

        from("direct:scope-proxy")
                .id("scopeProxyRoute")
                .log(LoggingLevel.INFO, "Searching articles in Scope")
                .process("scopeServiceProcessor");

        from("direct:search-articles-service")
                .id("searchArticleService")
                .multicast(new SourceArticlesAggregator())
                    .parallelProcessing()
                    .to("direct:ebi-proxy", "direct:scope-proxy")
                .end();
    }
}
