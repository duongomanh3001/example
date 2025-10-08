'use client';

import { useEffect, useState } from 'react';

export default function ClientOnlyWrapper() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    
    // Remove any conflicting classes that might be added by browser extensions or scripts
    const htmlElement = document.documentElement;
    const bodyElement = document.body;
    
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
      if (bodyElement.classList.contains(className)) {
        bodyElement.classList.remove(className);
      }
    });

    // Remove any attributes that might cause hydration mismatch
    const attributesToRemove = ['cz-shortcut-listen'];
    attributesToRemove.forEach(attr => {
      if (bodyElement.hasAttribute(attr)) {
        bodyElement.removeAttribute(attr);
      }
    });
  }, []);

  // Don't render anything on server side to avoid hydration mismatch
  if (!mounted) return null;

  return null; // This component doesn't render anything
}
