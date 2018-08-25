import * as React from 'react';

import './Footer.css';

export const Footer = () => (
  <div className="footer">
    <hr />
    <div className="footer__text">
      <div className="footer__left">&copy; 2018 Ikatan Alumni TOKI</div>
      <div className="footer__right">
        Powered by <a href="https://github.com/ia-toki/judgels">Judgels</a> 2.4.1
      </div>
    </div>
  </div>
);
