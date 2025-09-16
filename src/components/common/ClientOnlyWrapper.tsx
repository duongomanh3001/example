'use client';

import { useEffect } from 'react';

export default function ClientOnlyWrapper() {
  useEffect(() => {
    // Remove any conflicting classes that might be added by browser extensions or scripts
    const htmlElement = document.documentElement;
    
    // Remove mdl-js class if it exists (likely added by browser extension)
    if (htmlElement.classList.contains('mdl-js')) {
      htmlElement.classList.remove('mdl-js');
    }
    
    // Clean up any other potential conflicting classes
    const conflictingClasses = ['mdl-js', 'material-design-lite'];
    conflictingClasses.forEach(className => {
      if (htmlElement.classList.contains(className)) {
        htmlElement.classList.remove(className);
      }
    });
  }, []);

  return null; // This component doesn't render anything
}
