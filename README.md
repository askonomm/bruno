# Bruno

[![Release](https://github.com/askonomm/bruno/actions/workflows/release.yml/badge.svg)](https://github.com/askonomm/bruno/actions/workflows/release.yml)

A Jekyll-esque static site generator written in Clojure that, unlike most static site generators, uses Clojure for
templating as well thus being the perfect static site generator for those who love Clojure, or who would love to do
some _actual_ programming in their templating.

## Install

### Locally

```bash
curl -s https://raw.githubusercontent.com/askonomm/bruno/master/install.sh | bash -s
```

You can then run Bruno as `./bruno`, given that the Bruno executable is in the current working directory.

### Globally

```bash
curl -s https://raw.githubusercontent.com/askonomm/bruno/master/install.sh | bash -s -- -g
```

You can then run Bruno as `bruno` from anywhere.

## Usage

- [Directory structure](#)
- [Content items](#)
- [Layouts](#)
- [Pages](#)
- [Partials](#)
- [Templating data](#)
- [Templating helpers](#)
