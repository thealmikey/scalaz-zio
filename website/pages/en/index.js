/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

const CompLibrary = require('../../core/CompLibrary.js');

const MarkdownBlock = CompLibrary.MarkdownBlock; /* Used to read markdown */
const Container = CompLibrary.Container;
const GridBlock = CompLibrary.GridBlock;

const features = [
  {
    name: 'High-Performance',
    description: 'Build scalable applications with 100x the performance of Scala’s Future'
  },
  {
    name: '',
    description: ''
  },
];

class HomeSplash extends React.Component {
  render() {
    const {siteConfig, language = ''} = this.props;
    const {baseUrl, docsUrl} = siteConfig;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${language ? `${language}/` : ''}`;
    const docUrl = doc => `${baseUrl}${docsPart}${langPart}${doc}`;

    const SplashContainer = props => (
      <div className="homeContainer">
        <div className="homeSplashFade">
          <div className="wrapper homeWrapper">{props.children}</div>
        </div>
      </div>
    );
    const ProjectTitle = () => (
      <h2 className="projectTitle">
        {siteConfig.title}
        <small>{siteConfig.tagline}</small>
      </h2>
    );

    const PromoSection = props => (
      <div className="section promoSection">
        <div className="promoRow">
          <div className="pluginRowBlock">{props.children}</div>
        </div>
      </div>
    );

    const Button = props => (
      <div className="pluginWrapper buttonWrapper">
        <a className="button" href={props.href} target={props.target}>
          {props.children}
        </a>
      </div>
    );
    return (
      <SplashContainer>
        <div className="inner">
          <ProjectTitle siteConfig={siteConfig} />
          <PromoSection>
            <Button href="./docs/getting_started.html">Getting started</Button>
          </PromoSection>
        </div>
      </SplashContainer>
    );
  }
}

class Index extends React.Component {
  render() {
    const {config: siteConfig, language = ''} = this.props;
    const {baseUrl} = siteConfig;

    const Block = props => (
      <Container
        padding={['bottom', 'top']}
        id={props.id}
        background={props.background}>
        <GridBlock
          align="center"
          contents={props.children}
          layout={props.layout}
        />
      </Container>
    );

    const Features = () => (
      <Block layout="threeColumn">
        {[
          {
            content: 'Build scalable applications with 100x the performance of Scala’s Future',
            title: 'High-performance',
          },
          {
            content: 'Use the full power of the Scala compiler to catch bugs at compile time',
            title: 'Type-safe',
          },
          {
            content: 'Easily build concurrent apps without deadlocks, race conditions, or complexity',
            title: 'Concurrent',
          },
          {
            content: 'Write sequential code that looks the same whether it’s asynchronous or synchronous',
            title:'Asynchronous',
          },
          {
            content: 'Build apps that never leak resources (including threads!), even when they fail',
            title: 'Resource-safe',
          },
          {
            content: 'Inject test services into your app for fast, deterministic, and type-safe testing',
            title: 'Testable',
          },
          {
            content: 'Build apps that never lose errors, and which respond to failure locally and flexibly',
            title: 'Resilient',
          },
          {
            content: 'Rapidly compose solutions to complex problems from simple building blocks',
            title: 'Functional'
          },
        ]}
      </Block>
    );

    return (
      <div>
        <HomeSplash siteConfig={siteConfig} language={language} />
        <div className="mainContainer">
          <Features />
        </div>
      </div>
    );
  }
}

module.exports = Index;
