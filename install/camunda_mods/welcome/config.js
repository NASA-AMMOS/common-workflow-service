/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

window.camWelcomeConf = {
  // app: {
  //   name: 'Todos',
  //   vendor: 'Company'
  // },
  //
  // configure the date format
  // "dateFormat": {
  //   "normal": "LLL",
  //   "long":   "LLLL"
  // },
  //
  // "locales": {
  //    "availableLocales": ["en", "de"],
  //    "fallbackLocale": "en"
  //  },
  // links: [
  //   {
  //     label: 'Angular.js Docs',
  //     href: 'https://code.angularjs.org/1.2.16/docs',
  //     description: 'Almost interesting'
  //   },
  //   {
  //     label: 'XKCD',
  //     href: 'http://xkcd.org',
  //     description: 'Nerdy comic'
  //   },
  //   {
  //     label: 'Slashdot',
  //     href: 'https://slashdot.org',
  //     description: 'News for nerds, stuff that matter'
  //   }
  // ],
    customScripts: {
        // AngularJS module names
        ngDeps: [],
        //   // RequireJS configuration for a complete configuration documentation see:
        //   // http://requirejs.org/docs/api.html#config
        deps: ['jquery', 'cws'],
        paths: {
            // if you have a folder called `custom-ui` (in the `scripts` folder)
            // with a file called `scripts.js` in it and defining the `custom-ui` AMD module
            'cws': 'cws/scripts'
        }
    },
  // csrfCookieName: 'XSRF-TOKEN',
  // disableWelcomeMessage: false
};
